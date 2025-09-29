package it.pagopa.pn.portfat.config;

import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@ExtendWith(MockitoExtension.class)
class HttpConnectorWebClientImplTest {

    private ClientAndServer mockServer;
    private HttpConnectorWebClient httpConnectorWebClient;


    @BeforeEach
    void init() {
        mockServer = ClientAndServer.startClientAndServer(1585);
        WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);
        WebClient webClient = WebClient.builder().baseUrl("http://localhost:1585").build();
        when(webClientBuilder.build()).thenReturn(webClient);
        httpConnectorWebClient = new HttpConnectorWebClient(webClientBuilder);
    }

    @AfterEach
    void stopMockServer() {
        mockServer.stop();
    }


    @Test
    void testDownloadFileAsByteArray() throws Exception {
        String fakeContent = "Fake file content";
        //byte[] fakeBytes = new ClassPathResource("portfatt_modulo_commessa_2025_10.zip").getInputStream().readAllBytes();
        byte[] fakeBytes = fakeContent.getBytes(StandardCharsets.UTF_8);
        mockServer
                .when(request()
                        .withMethod("GET")
                        .withPath("/test.zip"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody(fakeBytes)
                        .withHeader("Content-Type", "application/octet-stream"));

        Path outputFile = Paths.get("test_downloaded.zip"); // file nella root del progetto

        try {
            Mono<Void> result = httpConnectorWebClient.downloadFileAsByteArray(
                    "http://localhost:1585/test.zip",
                    outputFile
            );
            result.block();

            assertTrue(Files.exists(outputFile), "Il file non è stato creato");
            assertTrue(Files.size(outputFile) > 0, "Il file è vuoto");
        } finally {
            //Files.deleteIfExists(tempFile);
        }
    }


    @Test
    void testDownloadFileNotFound() {
        mockServer
                .when(request().withMethod("GET").withPath("/notfound.zip"))
                .respond(response().withStatusCode(404).withBody("Not Found"));

        Path tempFile = Paths.get("test-notfound.zip");
        PnGenericException ex = (PnGenericException)assertThrows(Exception.class, () -> {
            httpConnectorWebClient.downloadFileAsByteArray("http://localhost:1585/notfound.zip", tempFile)
                    .block();
        });
        assertNotNull(ex);
    }

    @Test
    void testDownloadServerError() {
        mockServer
                .when(request().withMethod("GET").withPath("/servererror.zip"))
                .respond(response().withStatusCode(500).withBody("Internal Server Error"));

        Path tempFile = Paths.get("test-servererror.zip");
        PnGenericException ex = (PnGenericException)assertThrows(Exception.class, () -> {
            httpConnectorWebClient.downloadFileAsByteArray("http://localhost:1585/servererror.zip", tempFile)
                    .block();
        });
        assertNotNull(ex);
    }

    @Test
    void testUploadContent() {
        mockServer
                .when(HttpRequest.request().withMethod("POST").withPath("/upload"))
                .respond(HttpResponse.response().withStatusCode(200).withBody("File uploaded successfully"));

        FileCreationWithContentRequest fileCreationRequest = mock(FileCreationWithContentRequest.class);
        FileCreationResponseDto fileCreationResponse = mock(FileCreationResponseDto.class);
        when(fileCreationRequest.getContentType()).thenReturn("application/zip");
        when(fileCreationRequest.getContent()).thenReturn("fake content".getBytes());
        when(fileCreationResponse.getUploadUrl()).thenReturn("http://localhost:1585/upload");
        when(fileCreationResponse.getUploadMethod()).thenReturn(FileCreationResponseDto.UploadMethodEnum.POST);
        when(fileCreationResponse.getKey()).thenReturn("fake-key");
        when(fileCreationResponse.getSecret()).thenReturn("fake-secret");

        String sha256 = "fake-sha256";
        Mono<Void> result = httpConnectorWebClient.uploadContent(fileCreationRequest, fileCreationResponse, sha256);
        result.block();
        assertNotNull(result);
    }

    @Test
    void testUploadContentServerError() {
        mockServer
                .when(HttpRequest.request().withMethod("POST").withPath("/upload"))
                .respond(HttpResponse.response().withStatusCode(500).withBody("Internal Server Error"));

        FileCreationWithContentRequest fileCreationRequest = mock(FileCreationWithContentRequest.class);
        FileCreationResponseDto fileCreationResponse = mock(FileCreationResponseDto.class);
        when(fileCreationRequest.getContentType()).thenReturn("application/zip");
        when(fileCreationRequest.getContent()).thenReturn("fake content".getBytes());
        when(fileCreationResponse.getUploadUrl()).thenReturn("http://localhost:1585/upload");
        when(fileCreationResponse.getUploadMethod()).thenReturn(FileCreationResponseDto.UploadMethodEnum.POST);
        when(fileCreationResponse.getKey()).thenReturn("fake-key");
        when(fileCreationResponse.getSecret()).thenReturn("fake-secret");

        String sha256 = "fake-sha256";
        Exception ex = assertThrows(Exception.class, () -> {
            httpConnectorWebClient.uploadContent(fileCreationRequest, fileCreationResponse, sha256)
                    .block();
        });
        assertNotNull(ex);
    }
}