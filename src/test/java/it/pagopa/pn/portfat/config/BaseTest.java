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



@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(LocalStackTestConfig.class)
public abstract class BaseTest {

    @Slf4j
    @SpringBootTest
    @ActiveProfiles("test")
    @Import(LocalStackTestConfig.class)
    public static class WithLocalStack {
    }

    @Getter
    @Slf4j
    @SpringBootTest
    @ActiveProfiles("test")
    @Import(LocalStackTestConfig.class)
    public static class WithMockServer {

        @Autowired
        private MockServerBean mockServerBean;

        @BeforeEach
        public void init() {
            log.info("Starting tests in {}", this.getClass().getSimpleName());
            setExpection(this.getClass().getSimpleName() + ".json");
        }

        public void tearDown() {
            log.info("Stopping MockServer");
            this.mockServerBean.stop();
        }

        void setExpection(String file) {
            this.mockServerBean.initializationExpection(file);
        }
    }
}