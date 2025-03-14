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
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

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
    private final FileReadyEvent event = new FileReadyEvent();

    @BeforeEach
    void setup() throws Exception {
        amazonSQS.createQueue(new CreateQueueRequest().withQueueName(portFatPropertiesConfig.getQueue()));
        String queueUrl = amazonSQS.getQueueUrl(portFatPropertiesConfig.getQueue()).getQueueUrl();

        event.setDownloadUrl(portFatPropertiesConfig.getBlobStorageBaseUrl() + "/portfatt/invoices/portFatt.zip");
        event.setFileVersion("1.0");

        String messageBody = objectMapper.writeValueAsString(event);
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)
                .withMessageGroupId(MESSAGE_GROUP_ID);
        amazonSQS.sendMessage(sendMessageRequest);

        byte[] zipBytes = Files.readAllBytes(Paths.get("src/test/resources/portFatt.zip"));
        new MockServerClient("localhost", 1050)
                .when(request()
                        .withMethod("GET")
                        .withPath("/portfatt/invoices/portFatt.zip")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_OCTET_STREAM)
                        .withBody(zipBytes)
                );
    }

    @Test
    void testMessageReceptionAndProcessing() {
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> verify(portfatService, times(1)).processZipFile(any()));
    }
}
