package uk.ac.ebi.ddi.task.ddisearchcount;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.ac.ebi.ddi.task.ddisearchcount.service.EBISearchCountService;

@SpringBootApplication
public class DdiSearchCountApplication implements CommandLineRunner {

	@Autowired
	private EBISearchCountService ebiSearchCountService;

	public static void main(String[] args) {
		SpringApplication.run(DdiSearchCountApplication.class, args);
	}

	@Override
	public void run(String... args){
		ebiSearchCountService.saveSearchcounts();
	}

}
