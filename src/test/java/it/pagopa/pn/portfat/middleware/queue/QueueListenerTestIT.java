package it.pagopa.pn.portfat.middleware.queue;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.config.BaseTest;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.config.SqsTestConfig;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.service.PortFatService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.stream.Stream;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@Slf4j
@SpringBootTest(classes = { SqsTestConfig.class },
        properties = "spring.autoconfigure.exclude=io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration")
class QueueListenerTestIT extends BaseTest.WithMockServer {

    @Autowired
    @Qualifier("amazonSQSAsync")
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


    @BeforeEach
    void setup() throws Exception {
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
        Path baseZipDir = Paths.get(portFatPropertiesConfig.getBasePathZipFiele());
        Awaitility.await()
                .atMost(Duration.ofSeconds(120))
                .until(() -> {
                    try (Stream<Path> stream = Files.list(baseZipDir)) {
                        return stream.findAny().isEmpty();
                    } catch (IOException e) {
                        return !Files.exists(baseZipDir);
                    }
                });
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
