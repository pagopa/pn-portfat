package it.pagopa.pn.portfat.middleware.queue;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.config.BaseTest;
import it.pagopa.pn.portfat.config.WebClientTestConfig;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.service.PortFatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Disabled
class QueueListenerTestIT extends BaseTest.WithMockServer {

    @Autowired
    private AmazonSQSAsync amazonSQS;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebClientTestConfig webClientTestConfig;

    @Autowired
    private PortFatDownloadDAO portFatDownloadDAO;

    @SpyBean
    private PortFatService portfatService;

    private WebClient webClient;

    private static final int MOCK_SERVER_PORT = 1080;

    private static final String MESSAGE_GROUP_ID = "port-fat_1";
    private static final String QUEUE_NAME = "local-pn-portfat-inputs-requests.fifo";
    private String queueUrl;

    @BeforeEach
    void setup() throws JsonProcessingException {
        amazonSQS.createQueue(new CreateQueueRequest().withQueueName(QUEUE_NAME));
        queueUrl = amazonSQS.getQueueUrl(QUEUE_NAME).getQueueUrl();

        // Crea il messaggio da inviare alla coda
        FileReadyEvent event = new FileReadyEvent();
        event.setDownloadUrl("https://storage.portalefatturazione.pagopa.it/portfatt/pn-example_it/pn-example_it");
        event.setFileVersion("v1.0");
        String messageBody = objectMapper.writeValueAsString(event);

        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)
                .withMessageGroupId(MESSAGE_GROUP_ID);
        amazonSQS.sendMessage(sendMessageRequest);
        webClient = WebClient.create("http://localhost:" + MOCK_SERVER_PORT);
    }

    @Test
    void testMessageReceptionAndProcessing() {
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(portfatService, times(1)).processZipFile(any()));
    }
}
