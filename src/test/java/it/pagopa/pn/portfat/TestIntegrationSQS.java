package it.pagopa.pn.portfat;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.middleware.queue.QueueListener;
import it.pagopa.pn.portfat.model.FileReadyModel;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(properties = "mockserver.bean.port=1080")
@ContextConfiguration(classes = {LocalStackTestConfig.class})
class SqsIntegrationTest {

    @Autowired
    private QueueListener queueListener;


    @Test
    void integrationTestPullPortFat_withCustomSqsEvent() throws Exception {
        // Arrange: crea un evento SQS personalizzato
        Map<String, Object> headers = new HashMap<>();
        headers.put("id", UUID.randomUUID().toString());
        headers.put("X-Amzn-Trace-Id", "Root=1-67891233-abcdef012345678912345678");

        FileReadyModel event = new FileReadyModel();
        event.setFileVersion("v1.0");
        event.setDownloadUrl("https://storage.uat.portalefatturazione.pagopa.it/modulicommessazip/2025_10/portfatt_modulo_commessa_2025_10.zip?sp=r&st=2025-09-29T13:04:34Z&se=2026-09-01T21:19:34Z&spr=https&sv=2024-11-04&sr=b&sig=ytp6WgkAdJUQr%2FOoAp6segUkySv9U21bPILCRGyBomw%3D");
        event.setFilePath("/temp/invoices.zip");
        String payload = new ObjectMapper().writeValueAsString(event);

        // Act: invoca direttamente il listener
        queueListener.pullPortFat(payload, headers);

        // Assert: aggiungi qui le tue verifiche, ad esempio con Mockito.verify(...)
    }
}
