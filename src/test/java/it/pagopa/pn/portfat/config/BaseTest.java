package it.pagopa.pn.portfat.config;

import it.pagopa.pn.portfat.LocalStackTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import(LocalStackTestConfig.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

    @Slf4j
    @SpringBootTest
    @ActiveProfiles("test")
    @Import(LocalStackTestConfig.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public static class WithLocalStack {
    }

    @Slf4j
    @SpringBootTest
    @ActiveProfiles("test")
    @Import(LocalStackTestConfig.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public static class WithMockServer {

        @Autowired
        private MockServerBean mockServer;

        @BeforeAll
        public void init() {
            log.info("Starting tests in {}", this.getClass().getSimpleName());
            setExpection(this.getClass().getSimpleName() + ".json");
        }

        //@AfterAll
        public void tearDown() {
            log.info("Stopping MockServer");
            this.mockServer.stop();
        }

        void setExpection(String file) {
            this.mockServer.initializationExpection(file);
        }
    }
}