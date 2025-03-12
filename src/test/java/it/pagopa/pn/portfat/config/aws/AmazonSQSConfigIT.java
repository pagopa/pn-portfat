package it.pagopa.pn.portfat.config.aws;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import it.pagopa.pn.portfat.LocalStackTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AmazonSQSConfigIT extends LocalStackTestConfig {

    @Autowired
    private AmazonSQSAsync amazonSQS;

    @Autowired
    private AwsPropertiesConfig awsConfigs;

    @Value("${pn.portfat.queue}")
    private String queue;

    @SpyBean
    SQSConfig sqsConfig;

    @BeforeEach
    void setup() {
        amazonSQS.createQueue(queue);
    }

    @Test
    void testAmazonSQSClientCreation_WithCustomEndpoint() {
        // Simula configurazione con endpoint personalizzato
        AmazonSQSAsync sqsClient = sqsConfig.amazonSQS();
        assertNotNull(sqsClient);
        assertNotNull(sqsClient.getQueueUrl(queue));
    }

}