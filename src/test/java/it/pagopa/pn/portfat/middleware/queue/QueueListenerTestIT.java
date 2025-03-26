package it.pagopa.pn.portfat.middleware.queue;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.config.BaseTest;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.DownloadStatus;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.model.FileReadyModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@Slf4j
class QueueListenerTestIT extends BaseTest.WithMockServer {

    @Autowired
    private AmazonSQSAsync amazonSQS;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
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
        Path baseZipDir = Paths.get(portFatPropertiesConfig.getBasePathZipFile());

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

        String queueUrl = amazonSQS.getQueueUrl(portFatPropertiesConfig.getSqsQueue()).getQueueUrl();
        ReceiveMessageRequest request = new ReceiveMessageRequest()
                .withQueueUrl(queueUrl)
                .withMaxNumberOfMessages(1)
                .withWaitTimeSeconds(5);

        List<Message> messages = amazonSQS.receiveMessage(request).getMessages();
        assertTrue(messages.isEmpty());

        getMockServerBean().toVerify(request().withMethod("GET").withPath(filePath), VerificationTimes.atLeast(1));
        getMockServerBean().toVerify(request().withMethod("POST").withPath("/safe-storage/v1/files"), VerificationTimes.atLeast(1));
        getMockServerBean().toVerify(request().withMethod("PUT").withPath("/safe-storage/storage/invoice.json"), VerificationTimes.atLeast(1));
    }


    private void pushOnQueue(String fileVersion, String messageGroupId) throws IOException {
        String queueUrl = amazonSQS.getQueueUrl(portFatPropertiesConfig.getSqsQueue()).getQueueUrl();
        FileReadyModel event = new FileReadyModel();
        event.setDownloadUrl(fileUrl);
        event.setFileVersion(fileVersion);
        event.setFilePath(filePath);
        String messageBody = objectMapper.writeValueAsString(event);
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)
                .withMessageGroupId(messageGroupId);
        amazonSQS.sendMessage(sendMessageRequest);
    }

    void clearSqsQueue() {
        String queueUrl = amazonSQS.getQueueUrl(portFatPropertiesConfig.getSqsQueue()).getQueueUrl();
        List<Message> messages = amazonSQS.receiveMessage(
                new ReceiveMessageRequest()
                        .withQueueUrl(queueUrl)
                        .withMaxNumberOfMessages(10)
                        .withWaitTimeSeconds(1)
        ).getMessages();

        for (Message message : messages) {
            amazonSQS.deleteMessage(new DeleteMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withReceiptHandle(message.getReceiptHandle()));
        }
    }

    private void configMockServerMessageDequeuedAndProcessing() throws IOException {
        byte[] zipBytes = Files.readAllBytes(Paths.get("src/test/resources/portFatt.zip"));
        getMockServerBean().setRequestResponse(
                request().withMethod("GET").withPath(filePath),
                response().withStatusCode(200).withHeader("Content-Type", "application/x-zip-compressed").withHeader("Content-Disposition", "attachment; filename=\"file.zip\"").withBody(zipBytes)
        );
    }
}