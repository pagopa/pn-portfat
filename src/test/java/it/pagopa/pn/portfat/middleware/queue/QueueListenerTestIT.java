package it.pagopa.pn.portfat.middleware.queue;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.config.BaseTest;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.DownloadStatus;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.model.FileReadyModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@Slf4j
@Disabled
class QueueListenerTestIT extends BaseTest.WithMockServer {

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private PortFatDownloadDAO portFatDownloadDAO;

    @Autowired
    private PortFatPropertiesConfig portFatPropertiesConfig;

    private final String filePath = "/portfatt/invoices/portFatt.zip";

    private String fileUrl;


    @PostConstruct
    public void setup() {
        fileUrl = portFatPropertiesConfig.getBlobStorageBaseUrl() + filePath;
    }


    @BeforeEach
    void cleanBefore() {
        clearSqsQueue();
    }

    @AfterEach
    void cleanAfter() {
        clearSqsQueue();
        tearDown();
    }

    @Test
    void testMessageDequeuedAndProcessing() throws IOException {
        String fileVersion = "1.0";
        String messageGroupId = "port-fat_1";
        configMockServerMessageDequeuedAndProcessing();
        pushOnQueue(fileVersion, messageGroupId);

        Awaitility.await()
                .atMost(Duration.ofSeconds(40))
                .until(() -> {
                    PortFatDownload download = portFatDownloadDAO
                            .findByDownloadId(fileUrl + fileVersion)
                            .block();
                    return download != null && download.getStatus() == DownloadStatus.COMPLETED;
                });

        Optional<PortFatDownload> download = portFatDownloadDAO
                .findByDownloadId(fileUrl + fileVersion)
                .blockOptional();
        assertThat(download).isPresent();
        assertThat(download.get().getStatus()).isEqualTo(DownloadStatus.COMPLETED);

        String queueUrl = getQueueUrl();
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(5)
                .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();
        assertTrue(messages.isEmpty());

        getMockServerBean().toVerify(request().withMethod("GET").withPath(filePath), VerificationTimes.atLeast(1));
        getMockServerBean().toVerify(request().withMethod("POST").withPath("/safe-storage/v1/files"), VerificationTimes.atLeast(1));
        getMockServerBean().toVerify(request().withMethod("PUT").withPath("/safe-storage/storage/invoice.json"), VerificationTimes.atLeast(1));
    }

    private void pushOnQueue(String fileVersion, String messageGroupId) throws IOException {
        String queueUrl = getQueueUrl();
        FileReadyModel event = new FileReadyModel();
        event.setDownloadUrl(fileUrl);
        event.setFileVersion(fileVersion);
        event.setFilePath(filePath);
        String messageBody = objectMapper.writeValueAsString(event);
        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .messageGroupId(messageGroupId)
                .build();
        sqsClient.sendMessage(request);
    }

    void clearSqsQueue() {
        String queueUrl = getQueueUrl();
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(1)
                .build();
        List<Message> messages = sqsClient.receiveMessage(request).messages();

        for (Message message : messages) {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
            sqsClient.deleteMessage(deleteRequest);
        }
    }

    private String getQueueUrl() {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(portFatPropertiesConfig.getSqsQueue())
                .build()).queueUrl();
    }

    private void configMockServerMessageDequeuedAndProcessing() throws IOException {
        byte[] zipBytes = Files.readAllBytes(Paths.get("src/test/resources/portFatt.zip"));
        getMockServerBean().setRequestResponse(
                request().withMethod("GET").withPath(filePath),
                response().withStatusCode(200).withHeader("Content-Type", "application/x-zip-compressed").withHeader("Content-Disposition", "attachment; filename=\"file.zip\"").withBody(zipBytes)
        );
    }
}