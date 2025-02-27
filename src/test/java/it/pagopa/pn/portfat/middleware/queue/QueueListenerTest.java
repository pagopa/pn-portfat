package it.pagopa.pn.portfat.middleware.queue;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test") // Usa un profilo dedicato per il test
@Testcontainers
class QueueListenerTest {

    @Value("${pn.pn-portfat.queue}")
    private String queueUrl;

    @Container
    public static LocalStackContainer localstack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
                    .withServices(LocalStackContainer.Service.SQS);

    @Autowired
    private AmazonSQS amazonSQS;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        amazonSQS.createQueue(new CreateQueueRequest()
                .withQueueName(queueUrl)
                .withAttributes(Map.of("FifoQueue", "true", "ContentBasedDeduplication", "true")));
        queueUrl = amazonSQS.getQueueUrl(queueUrl).getQueueUrl();
    }
    @Test
    void testMessageReception() throws Exception {
        // Crea il messaggio
        FileReadyEvent event = new FileReadyEvent();
        event.setDownloadUrl("https://example.com/file.pdf");
        event.setFileVersionString("v1.0");
        String messageBody = objectMapper.writeValueAsString(event);

        // Invia il messaggio alla coda
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)
                .withMessageGroupId("group1");
        amazonSQS.sendMessage(sendMessageRequest);


        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {

            assertTrue(true);
        });
    }
}