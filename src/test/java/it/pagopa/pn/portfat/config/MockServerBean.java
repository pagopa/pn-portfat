package it.pagopa.pn.portfat.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import java.io.IOException;
import java.time.Duration;


@Slf4j
public class MockServerBean {
    @Getter
    private ClientAndServer mockServer;
    private final int port;

    public MockServerBean(int port){
        this.port = port;
        log.info("Mock server started on : {}", port);
    }

    public void stop(){
        this.mockServer.stop();
    }

    public void initializationExpection(String file){
        log.info("- Initialize Mock Server Expection");
        Resource resource = new ClassPathResource(file);
        try {
            String path = resource.getFile().getAbsolutePath();
            log.info(" - Path : {} ", path);
            ConfigurationProperties.initializationJsonPath(path);
            this.mockServer = ClientAndServer.startClientAndServer(port);
            Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> mockServer.isRunning());
        } catch (IOException e) {
            log.error(" - File json not found");
        }
    }
}
