package it.pagopa.pn.portfat.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RequestDefinition;
import org.mockserver.verify.VerificationTimes;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import java.io.IOException;


@Slf4j
@Getter
public class MockServerBean {
    private ClientAndServer mockServer;
    private final int port;

    public MockServerBean(int port) {
        this.port = port;
    }

    public void stop() {
        try {
            if (this.mockServer != null && this.mockServer.isRunning()) {
                this.mockServer.reset();
            }
        } finally {
            assert this.mockServer != null;
            this.mockServer.stop();
            log.info("Mock server stopped on: {}", mockServer.getPort());
        }
    }

    public void initializationExpection(String file){
        log.info("- Initialize Mock Server Expection");
        Resource resource = new ClassPathResource(file);
        try {
            String path = resource.getFile().getAbsolutePath();
            log.info(" - Path : {} ", path);
            ConfigurationProperties.initializationJsonPath(path);
            this.mockServer = ClientAndServer.startClientAndServer(port);
            log.info("Mock server started on: {}", port);
        } catch (IOException e) {
            log.error(" - File json not found");
        }
    }

    public void setRequestResponse(RequestDefinition requestDefinition, HttpResponse httpResponse) {
        this.mockServer.when(requestDefinition).respond(httpResponse);
    }

    public void toVerify(RequestDefinition requestDefinition, VerificationTimes times) {
        this.mockServer.verify(requestDefinition, times);
    }
}