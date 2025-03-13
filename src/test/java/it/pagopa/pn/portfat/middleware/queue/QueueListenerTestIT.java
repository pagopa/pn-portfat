package it.pagopa.pn.portfat.middleware.queue;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.config.BaseTest;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.service.PortFatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class QueueListenerTestIT extends BaseTest.WithMockServer {

    @Autowired
    private AmazonSQSAsync amazonSQS;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortFatDownloadDAO portFatDownloadDAO;

    @SpyBean
    private PortFatService portfatService;

    @Value("${pn.portfat.blobStorageBaseUrl}")
    private String blobStorageBaseUrl;

    @Value("${pn.portfat.queue}")
    private String QUEUE_NAME;

    private static final String MESSAGE_GROUP_ID = "port-fat_1";

    private final AtomicReference<Mono<Void>> processingMonoRef = new AtomicReference<>();


    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            if (result instanceof Mono) {
                processingMonoRef.set((Mono<Void>) result);
            }
            return result;
        }).when(portfatService).processZipFile(any());

        amazonSQS.createQueue(new CreateQueueRequest().withQueueName(QUEUE_NAME));
        String queueUrl = amazonSQS.getQueueUrl(QUEUE_NAME).getQueueUrl();

        FileReadyEvent event = new FileReadyEvent();
        event.setDownloadUrl(blobStorageBaseUrl + "/portfatt/invoices/portFatt.zip");
        event.setFileVersion("1.0");

        String messageBody = objectMapper.writeValueAsString(event);
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)
                .withMessageGroupId(MESSAGE_GROUP_ID);
        amazonSQS.sendMessage(sendMessageRequest);
    }

    @Test
    void testMessageReceptionAndProcessing() {
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .until(() -> processingMonoRef.get() != null);


        verify(portfatService, times(1)).processZipFile(any());

        processingMonoRef.get().block();
    }
}