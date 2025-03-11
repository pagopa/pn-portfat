package it.pagopa.pn.portfat.middleware.queue;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.config.BaseTest;
import it.pagopa.pn.portfat.config.HttpConnectorWebClient;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import it.pagopa.pn.portfat.utils.Utility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
class QueueListenerTestIT extends BaseTest {

    @Value("${pn.portfat.queue}")
    private String queueUrl;

    @Autowired
    private AmazonSQSAsync amazonSQS;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private QueueListener queueListener;

    @MockBean
    private HttpConnectorWebClient httpConnectorWebClient;

    @MockBean
    private WebClient webClient;

    @MockBean
    private WebClient.RequestHeadersUriSpec requestHeadersSpec;

    @MockBean
    private WebClient.ResponseSpec responseSpec;

    @MockBean
    private DataBufferUtils dataBufferUtils;
    MockedStatic<Utility> utility;

    private static final String MESSAGE_GROUP_ID = "port-fat_1";
    private static final String TEST_ZIP_FILE_PATH = "src/test/resources/portFatt.zip";

    @BeforeEach
    void setup() throws JsonProcessingException {
        amazonSQS.createQueue(new CreateQueueRequest().withQueueName(queueUrl));
        queueUrl = amazonSQS.getQueueUrl(queueUrl).getQueueUrl();

        // Crea il messaggio da inviare
        FileReadyEvent event = new FileReadyEvent();
        event.setDownloadUrl("https://portale-fatturazione-storage.blob.core.windows.net/portfatt/pn-example_it");
        event.setFileVersion("v1.0");
        String messageBody = objectMapper.writeValueAsString(event);

        // Invia il messaggio alla coda
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)
                .withMessageGroupId(MESSAGE_GROUP_ID);
        amazonSQS.sendMessage(sendMessageRequest);

    }

    @Test
    void testMessageReception() throws IOException {
        // Mock del WebClient
        Mockito.when(httpConnectorWebClient.downloadFileAsByteArray(any(), any()))
                .thenReturn(Mono.empty());


        // Simula il download restituendo i byte del file ZIP
        Path testZipPath = Paths.get(TEST_ZIP_FILE_PATH);
        byte[] zipBytes = Files.readAllBytes(testZipPath);
        Flux<DataBuffer> mockDataBufferFlux = Flux.just(new DefaultDataBufferFactory().wrap(zipBytes));
        Mockito.when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(mockDataBufferFlux);


        await().atMost(Duration.ofSeconds(10)).untilAsserted(
                        () -> verify(queueListener, times(2)).pullPortFat(any(), any())
                );
    }
}
