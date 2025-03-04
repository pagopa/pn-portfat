package it.pagopa.pn.portfat.middleware.queue;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

//@SpringBootTest
//@ActiveProfiles("test")
//@Testcontainers
class QueueListenerTest {

    @Value("${pn.pn-portfat.queue}")
    private String queueUrl;

    @Container
    public static LocalStackContainer localstack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
                    .withServices(LocalStackContainer.Service.SQS);

    @Autowired
    private AmazonSQSAsync amazonSQS;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private QueueListener queueListener;

    private String messageBody;

    @BeforeEach
    void setup() throws JsonProcessingException {
        amazonSQS.createQueue(new CreateQueueRequest()
                .withQueueName(queueUrl));
        queueUrl = amazonSQS.getQueueUrl(queueUrl).getQueueUrl();
        // Crea il messaggio
        FileReadyEvent event = new FileReadyEvent();
        event.setDownloadUrl("https://example.com/file.pdf");
        event.setFileVersionString("v1.0");
        messageBody = objectMapper.writeValueAsString(event);

        // Invia il messaggio alla coda
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody);
        amazonSQS.sendMessage(sendMessageRequest);
    }

    //@Test
    void testMessageReception() {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            verify(queueListener, times(1)).pullPortFat(messageBody)
        );
    }
}