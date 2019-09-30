package uk.ac.ebi.ddi.task.ddisearchcount.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ddi.ddidomaindb.annotation.Constants;
import uk.ac.ebi.ddi.ddidomaindb.dataset.DSField;
import uk.ac.ebi.ddi.ebe.ws.dao.client.dataset.DatasetWsClient;
import uk.ac.ebi.ddi.ebe.ws.dao.client.domain.DomainWsClient;
import uk.ac.ebi.ddi.ebe.ws.dao.config.EbeyeWsConfigProd;
import uk.ac.ebi.ddi.service.db.model.dataset.Dataset;
import uk.ac.ebi.ddi.service.db.model.dataset.Scores;
import uk.ac.ebi.ddi.service.db.model.similarity.EBISearchPubmedCount;
import uk.ac.ebi.ddi.service.db.service.dataset.DatasetService;
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.QueryResult;
import uk.ac.ebi.ddi.ebe.ws.dao.model.domain.DomainList;
import uk.ac.ebi.ddi.ebe.ws.dao.model.domain.Domain;
import uk.ac.ebi.ddi.service.db.service.similarity.IEBIPubmedSearchService;
import uk.ac.ebi.ddi.ebe.ws.dao.model.common.Entry;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EBISearchCountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EBISearchCountService.class);

    private Integer startDataset = 0;

    private Integer numberOfDataset = 2000;

    private Integer numberOfCitations = 500;

    @Autowired
    private DatasetService datasetService;

    private DomainWsClient domainWsClient;

    private DatasetWsClient datasetWsClient;

    public EBISearchCountService(){
        domainWsClient = new DomainWsClient(new EbeyeWsConfigProd());
        datasetWsClient = new DatasetWsClient(new EbeyeWsConfigProd());
    }

    @Autowired
    private IEBIPubmedSearchService ebiPubmedSearchService;

    public void saveSearchcounts() {
        try {
            for (int i = startDataset; i < datasetService.getDatasetCount() / numberOfDataset; i = i + 1) {
                datasetService.readAll(i, numberOfDataset).getContent()
                        .parallelStream()
                        .map(data -> {
                            if (data.getCrossReferences() != null
                                    && data.getCrossReferences().get(DSField.CrossRef.PUBMED.key()) != null) {
                                data.getCrossReferences().get(DSField.CrossRef.PUBMED.key())
                                        .forEach(dta -> addSearchCounts(data.getAccession(), dta, data.getDatabase()));
                            } else {
                                addSearchCounts(data.getAccession(), "", data.getDatabase());
                            }
                            return "";
                        }).count();

            }
        } catch (Exception ex) {
            LOGGER.error("error inside savesearch count exception message is " + ex.getMessage());
        }
    }

    public void addSearchCounts(String accession, String pubmedId, String database) {
        LOGGER.info("inside add search counts ");
        int size = 20;
        int searchCount;
        try {
            HashMap<String, Integer> domainMap = new HashMap<>();
            Dataset dataset = datasetService.read(accession, database);
            List<String> filteredDomains = new ArrayList<>();
            Set<String> secondaryAccession = dataset.getAdditional().get(DSField.Additional.SECONDARY_ACCESSION.key());
            String query = pubmedId;
            query = (query == null || query.isEmpty()) ? "*:*" : query;

            QueryResult queryResult = null;

            DomainList domainList = domainWsClient.getDomainByName(Constants.OMICS_DOMAIN);

            List<String> domains = Arrays.stream(domainList.list).map(Domain::getId).collect(Collectors.toList());

            domains.add(Constants.ATLAS_GENES);
            domains.add(Constants.ATLAS_GENES_DIFFERENTIAL);
            domains.add(Constants.METABOLIGHTS);

            if (!pubmedId.equals("") && !pubmedId.equals("none") && !pubmedId.equals("0")) {
                query = "PUBMED:" + query + " OR MEDLINE:" + query + " OR PMID:" + query;
                queryResult = datasetWsClient.getDatasets(Constants.ALL_DOMAIN, query,
                        Constants.DATASET_SUMMARY, Constants.PUB_DATE_FIELD, "descending", 0, size, 10);
            }

            QueryResult queryAccessionResult = datasetWsClient.getDatasets(Constants.ALL_DOMAIN, accession,
                    Constants.DATASET_SUMMARY, Constants.PUB_DATE_FIELD, "descending", 0, size, 10);


            searchCount = queryResult != null && queryResult.getCount() > 0 ? queryResult.getDomains()
                    .stream()
                    .flatMap(dtl -> Arrays.stream(dtl.getSubdomains()))
                    .map(dtl -> Arrays.stream(dtl.getSubdomains()))
                    .flatMap(sbdt -> sbdt.filter(dt -> !domains.contains(dt.getId())))
                    .mapToInt(dtf -> {
                        updateKeyValue(dtf.getId().toLowerCase(), dtf.getHitCount(), domainMap);
                        //filteredDomains.add(dtf.getId() + "~" +dtf.getHitCount());
                        return dtf.getHitCount(); })
                    .sum() : 0;

            if (queryAccessionResult != null && queryAccessionResult.getCount() > 0) {
                searchCount = searchCount + queryAccessionResult.getDomains()
                        .parallelStream()
                        .flatMap(dtl -> Arrays.stream(dtl.getSubdomains()))
                        .map(dtl -> Arrays.stream(dtl.getSubdomains()))
                        .flatMap(sbdt -> sbdt.filter(dt -> !domains.contains(dt.getId())))
                        .mapToInt(dtf -> {
                            updateKeyValue(dtf.getId().toLowerCase(), dtf.getHitCount(), domainMap);
                            //filteredDomains.add(dtf.getId() + "~" +dtf.getHitCount());
                            return dtf.getHitCount(); })
                        .sum();
            }

            int allCounts = secondaryAccession != null ? secondaryAccession.parallelStream().mapToInt(dt -> {
                QueryResult querySecondaryResult = datasetWsClient.getDatasets(Constants.ALL_DOMAIN, dt,
                        Constants.DATASET_SUMMARY, Constants.PUB_DATE_FIELD, "descending", 0, size, 10);
                return querySecondaryResult.getDomains()
                        .stream()
                        .flatMap(dtl -> Arrays.stream(dtl.getSubdomains()))
                        .map(dtl -> Arrays.stream(dtl.getSubdomains()))
                        .flatMap(sbdt -> sbdt.filter(dtls -> !domains.contains(dtls.getId())))
                        .mapToInt(dtf -> {
                            updateKeyValue(dtf.getId().toLowerCase(), dtf.getHitCount(), domainMap);
                            //filteredDomains.add(dtf.getId() + "~" +dtf.getHitCount());
                            return dtf.getHitCount(); })
                        .sum();
                //return querySecondaryResult.getCount();
            }).sum() : 0;

            searchCount = searchCount + allCounts;
            //queryResult.getCount();

            Set<String> matchDataset = new HashSet<>();
            if (queryResult != null && queryResult.getEntries() != null) {
                matchDataset = Arrays.stream(queryResult.getEntries())
                        .filter(dt -> !dt.getId().equals(accession))
                        .map(Entry::getId).collect(Collectors.toSet());
            }

            if (dataset.getCrossReferences() != null) {
                Collection<Set<String>> crossReferences = dataset.getCrossReferences().values();
                searchCount = searchCount + crossReferences.stream().mapToInt(Set::size).sum();
                dataset.getCrossReferences().keySet().forEach(dt -> {
                    updateKeyValue(dt.toLowerCase(), dataset.getCrossReferences().get(dt).size(), domainMap);
                });

            }

            Set<String> domainSet =  domainMap.entrySet().parallelStream()
                    .map(dt -> dt.getKey() + "~" + dt.getValue()).collect(Collectors.toSet());
            EBISearchPubmedCount ebiSearchPubmedCount = new EBISearchPubmedCount();
            ebiSearchPubmedCount.setAccession(accession);
            ebiSearchPubmedCount.setPubmedCount(searchCount);
            Map<String, Set<String>> pubmedDatasets = new HashMap<String, Set<String>>();
            pubmedDatasets.put(pubmedId, matchDataset);
            ebiSearchPubmedCount.setPubmedDatasetList(pubmedDatasets);
            ebiPubmedSearchService.saveEbiSearchPubmed(ebiSearchPubmedCount);
            //Dataset dataset = datasetService.read(accession,database);
            if (dataset.getScores() != null) {
                dataset.getScores().setSearchCount(searchCount);
            } else {
                Scores scores = new Scores();
                scores.setSearchCount(searchCount);
                dataset.setScores(scores);
            }
            HashSet<String> count = new HashSet<>();
            count.add(String.valueOf(searchCount));
            dataset.getAdditional().put(DSField.Additional.SEARCH_COUNT.key(), count);
            dataset.getAdditional().put(DSField.Additional.SEARCH_DOMAIN.key(), domainSet);
            datasetService.update(dataset.getId(), dataset);


        } catch (Exception ex) {
            LOGGER.error("Exception occurred, query is " + pubmedId + " dataset is  " + accession + ", ", ex);
        }
    }

    public Map<String, Integer> updateKeyValue(String key, Integer value, Map<String, Integer> domainMap) {
        if (domainMap.containsKey(key)) {
            Integer updatedValue = domainMap.get(key);
            updatedValue = updatedValue + value;
            domainMap.put(key, updatedValue);
        } else {
            domainMap.put(key, value);
        }
        return domainMap;
    }


}
