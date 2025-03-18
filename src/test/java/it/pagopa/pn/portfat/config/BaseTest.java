package it.pagopa.pn.portfat.config;

import it.pagopa.pn.portfat.LocalStackTestConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import java.time.Duration;


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

    @Getter
    @Slf4j
    @SpringBootTest
    @ActiveProfiles("test")
    @Import(LocalStackTestConfig.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public static class WithMockServer {

        @Autowired
        private MockServerBean mockServerBean;

        @BeforeAll
        public void init() {
            log.info("Starting tests in {}", this.getClass().getSimpleName());
            setExpection(this.getClass().getSimpleName() + ".json");
            Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> mockServerBean.getMockServer().isRunning());
        }

        @AfterAll
        public void tearDown() {
            log.info("Stopping MockServer");
            this.mockServerBean.stop();
        }

        void setExpection(String file) {
            this.mockServerBean.initializationExpection(file);
        }
    }
}