package it.pagopa.pn.portfat.config;

import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
class HttpConnectorWebClientTest {

    private ClientAndServer mockServer;
    private HttpConnectorWebClient httpConnectorWebClient;

    @BeforeEach
    public void init() {
        mockServer = ClientAndServer.startClientAndServer(1585);
        // Mock di WebClient.Builder
        WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);
        WebClient webClient = WebClient.builder().baseUrl("http://localhost:1080").build();
        // Configura il mock per restituire il WebClient
        when(webClientBuilder.build()).thenReturn(webClient);
        // Inizializza HttpConnectorWebClient con il builder mockato
        httpConnectorWebClient = new HttpConnectorWebClient(webClientBuilder);
    }

    @AfterEach
    void stopMockServer() {
        mockServer.stop();
    }

    @Test
    void testDownloadFileAsByteArray() throws Exception {
        // Simula una risposta HTTP 200 con un contenuto fittizio
        mockServer
                .when(request().withMethod("GET").withPath("/test.zip"))
                .respond(response().withStatusCode(200)
                        .withBody("Fake file content")
                        .withHeader("Content-Type", "application/octet-stream"));

        Path tempFile = Files.createTempFile("test", ".zip");

        Mono<Void> result = httpConnectorWebClient.downloadFileAsByteArray("http://localhost:1080/test.zip", tempFile);
        result.block(); // Esegui e attendi il completamento

        // Verifica che il file sia stato scritto
        assertTrue(Files.exists(tempFile));
        assertTrue(Files.size(tempFile) > 0);

        // Cleanup
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testUploadContent() {
        // Simula una risposta HTTP 200 per l'upload
        mockServer
                .when(HttpRequest.request().withMethod("POST")
                        .withPath("/upload"))
                .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody("File uploaded successfully"));

        // Simula i dati per l'upload
        FileCreationWithContentRequest fileCreationRequest = mock(FileCreationWithContentRequest.class);
        FileCreationResponseDto fileCreationResponse = mock(FileCreationResponseDto.class);
        when(fileCreationRequest.getContentType()).thenReturn("application/zip");
        when(fileCreationRequest.getContent()).thenReturn("fake content".getBytes());
        when(fileCreationResponse.getUploadUrl()).thenReturn("http://localhost:1585/upload");
        when(fileCreationResponse.getUploadMethod()).thenReturn(FileCreationResponseDto.UploadMethodEnum.POST);
        when(fileCreationResponse.getKey()).thenReturn("fake-key");
        when(fileCreationResponse.getSecret()).thenReturn("fake-secret");

        String sha256 = "fake-sha256";

        // Esegui l'upload
        Mono<Void> result = httpConnectorWebClient.uploadContent(fileCreationRequest, fileCreationResponse, sha256);
        result.block(); // Esegui e attendi il completamento
        assertNotNull(result);
    }

}