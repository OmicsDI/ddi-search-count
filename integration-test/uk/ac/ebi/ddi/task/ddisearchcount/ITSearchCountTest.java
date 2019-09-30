package uk.ac.ebi.ddi.task.ddisearchcount;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DdiSearchCountApplication.class,
        initializers = ConfigFileApplicationContextInitializer.class)
public class ITSearchCountTest {

    @Autowired
    private DdiSearchCountApplication searchCountApplication;

    @Test
    public void contextLoads() throws Exception {
        searchCountApplication.run();
    }
}
