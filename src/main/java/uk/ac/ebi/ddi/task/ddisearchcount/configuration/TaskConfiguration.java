package uk.ac.ebi.ddi.task.ddisearchcount.configuration;

import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableTask
@Configuration
@EnableMongoRepositories({"uk.ac.ebi.ddi.service.db.repo"})
@ComponentScan({"uk.ac.ebi.ddi.service.db.service","uk.ac.ebi.ddi.ebe.ws.dao"})
public class TaskConfiguration {

}
