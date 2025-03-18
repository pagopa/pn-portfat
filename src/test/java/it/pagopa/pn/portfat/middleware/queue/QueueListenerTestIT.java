package it.pagopa.pn.portfat.middleware.queue;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.config.BaseTest;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.DownloadStatus;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class QueueListenerTestIT extends BaseTest.WithMockServer {

    @Autowired
    private AmazonSQSAsync amazonSQS;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private PortFatDownloadDAO portFatDownloadDAO;

    @Autowired
    private PortFatPropertiesConfig portFatPropertiesConfig;

    private static final String MESSAGE_GROUP_ID = "port-fat_1";

    private final String fileVersion = "1.0";
    private String fileUrl;

    @BeforeEach
    void setup() throws Exception {
        String filePath = "/portfatt/invoices/portFatt.zip";
        fileUrl = portFatPropertiesConfig.getBlobStorageBaseUrl() + filePath;

        amazonSQS.createQueue(new CreateQueueRequest().withQueueName(portFatPropertiesConfig.getSqsQueue()));
        String queueUrl = amazonSQS.getQueueUrl(portFatPropertiesConfig.getSqsQueue()).getQueueUrl();

        FileReadyEvent event = new FileReadyEvent();
        event.setDownloadUrl(fileUrl);
        event.setFileVersion(fileVersion);

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
        Path baseZipDir = Paths.get(portFatPropertiesConfig.getBasePathZipFile());

        try {
            Awaitility.await()
                    .atMost(Duration.ofSeconds(40))
                    .until(() -> {
                        PortFatDownload download = portFatDownloadDAO
                                .findByDownloadId(fileUrl + fileVersion)
                                .block();
                        return download != null && download.getStatus() == DownloadStatus.COMPLETED;
                    });

            Awaitility.await()
                    .atMost(Duration.ofSeconds(25))
                    .until(() -> {
                        try (Stream<Path> stream = Files.list(baseZipDir)) {
                            return stream.noneMatch(Files::isDirectory);
                        } catch (IOException e) {
                            return true;
                        }
                    });

            Optional<PortFatDownload> download = portFatDownloadDAO
                    .findByDownloadId(fileUrl + fileVersion)
                    .blockOptional();
            assertThat(download).isPresent();
            assertThat(download.get().getStatus()).isEqualTo(DownloadStatus.COMPLETED);
        } finally {
            super.tearDown();
        }
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