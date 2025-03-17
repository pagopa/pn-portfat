package it.pagopa.pn.portfat.middleware.queue;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.config.BaseTest;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.service.PortFatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import reactor.core.publisher.Mono;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


class QueueListenerTestIT extends BaseTest.WithMockServer {

    @Autowired
    private AmazonSQSAsync amazonSQS;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private PortFatDownloadDAO portFatDownloadDAO;

    @SpyBean
    private PortFatService portfatService;

    @Autowired
    private PortFatPropertiesConfig portFatPropertiesConfig;

    private static final String MESSAGE_GROUP_ID = "port-fat_1";

    private final AtomicReference<Mono<Void>> processingMonoRef = new AtomicReference<>();


    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            if (result instanceof Mono) {
                if (processingMonoRef.get() == null) {
                    processingMonoRef.set(Mono.from((Mono<?>) result).then());
                }
                return result;
            }
            return result;
        }).when(portfatService).processZipFile(any());

        amazonSQS.createQueue(new CreateQueueRequest().withQueueName(portFatPropertiesConfig.getSqsQueue()));
        String queueUrl = amazonSQS.getQueueUrl(portFatPropertiesConfig.getSqsQueue()).getQueueUrl();

        FileReadyEvent event = new FileReadyEvent();
        event.setDownloadUrl(portFatPropertiesConfig.getBlobStorageBaseUrl() + "/portfatt/invoices/portFatt.zip");
        event.setFileVersion("1.0");

        String messageBody = objectMapper.writeValueAsString(event);
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)
                .withMessageGroupId(MESSAGE_GROUP_ID);
        amazonSQS.sendMessage(sendMessageRequest);
    }

    @Test
    void testMessageReceptionAndProcessing() throws IOException {
        configMockServer();

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .until(() -> processingMonoRef.get() != null);

        verify(portfatService, times(1)).processZipFile(any());

        processingMonoRef.get().block();
    }


    private void configMockServer() throws IOException {
        byte[] zipBytes = Files.readAllBytes(Paths.get("src/test/resources/portFatt.zip"));
        super.getMockServerBean()
                .getMockServer()
                .when(request()
                        .withMethod("GET")
                        .withPath("/portfatt/invoices/portFatt.zip")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/x-zip-compressed")
                        .withHeader("Content-Disposition", "attachment; filename=\"file.zip\"")
                        .withBody(zipBytes)
                );
    }
}