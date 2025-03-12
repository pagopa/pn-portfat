package it.pagopa.pn.portfat.config;

import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import it.pagopa.pn.commons.utils.metrics.SpringAnalyzer;
import it.pagopa.pn.portfat.LocalStackTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(LocalStackTestConfig.class)
@Testcontainers
public abstract class BaseTest {

    @MockBean
    private SpringAnalyzer springAnalyzer;

    /**
     * Subclasses need to be annotated with:
     * SpringBootTest
     * EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
     * ActiveProfiles(“test”)
     */
    public static class WithOutLocalStackTest {

        @MockBean
        private SpringAnalyzer springAnalyzer;

    }


    @Slf4j
    @SpringBootTest
    @EnableAutoConfiguration(exclude = {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
    @ActiveProfiles("test")
    public static class WithMockServer extends WithOutLocalStackTest {
        @Autowired
        private MockServerBean mockServer;

        /**
         * Subclasses need to be annotated with:
         * SpringBootTest
         * EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
         * ActiveProfiles(“test”)
         */
        public static class WithOutLocalStackTest {

            @MockBean
            private SpringAnalyzer springAnalyzer;

        }

        @BeforeEach
        public void init() {
            log.info(this.getClass().getSimpleName());
            setExpection(this.getClass().getSimpleName() + "-webhook.json");
        }

        @AfterEach
        public void kill() {
            log.info("Killed");
            this.mockServer.stop();
        }

        public void setExpection(String file) {
            this.mockServer.initializationExpection(file);
        }
    }


}
