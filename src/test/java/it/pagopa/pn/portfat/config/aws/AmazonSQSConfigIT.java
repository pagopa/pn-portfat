package it.pagopa.pn.portfat.config.aws;

import it.pagopa.pn.portfat.LocalStackTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AmazonSQSConfigIT extends LocalStackTestConfig {

    @Autowired
    private SqsClient sqsClient;

    @Value("${pn.portfat.sqsQueue}")
    private String sqsQueue;

    @BeforeEach
    void setup() {
        sqsClient.createQueue(CreateQueueRequest.builder()
                .queueName(sqsQueue)
                .build());
    }

    @Test
    void testAmazonSQSClientCreation_WithCustomEndpoint() {
        // Simula configurazione con endpoint personalizzato
        String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(sqsQueue)
                        .build())
                .queueUrl();
        assertNotNull(sqsClient);
        assertNotNull(queueUrl);
    }
}