package it.pagopa.pn.portfat.middleware.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.config.BaseTest;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.DownloadStatus;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.model.FileReadyModel;
import it.pagopa.pn.portfat.service.PortFatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@SpringBootTest
@ActiveProfiles("test")
class QueueListenerTest extends BaseTest.WithLocalStack {

    @Autowired
    private QueueListener queueListener;

    @MockBean
    private PortFatPropertiesConfig portFatConfig;

    @MockBean
    private PortFatService portFatService;

    @MockBean
    private PortFatDownloadDAO portFatDownloadDAO;

    @MockBean
    private PortFatDownload portFatDownload;

    private String payload;
    Map<String, Object> headers;
    private static final String DOWNLOAD_URL = "https://portale-fatturazione-storage.blob.core.windows.net/portfatt/pn-example_it";
    private static final String FILE_VERSION = "v1.0";
    private static final String FILE_PATH = "/temp/invoices.zip";


    @BeforeEach
    void setup() {
        // Mock della configurazione
        when(portFatConfig.getBlobStorageBaseUrl())
                .thenReturn("https://portale-fatturazione-storage.blob.core.windows.net/portfatt/");
        when(portFatConfig.getFilePathWhiteList())
                .thenReturn(List.of("pn-example_it"));
    }

    @Test
    void testPullPortFatNewDownload() throws JsonProcessingException {
        //ARRANGE
        headers = new HashMap<>();
        headers.put("id", UUID.randomUUID());
        FileReadyModel event = new FileReadyModel();
        event.setFileVersion(FILE_VERSION);
        event.setDownloadUrl(DOWNLOAD_URL);
        event.setFilePath(FILE_PATH);
        payload = new ObjectMapper().writeValueAsString(event);

        // Mock di portFatDownloadDAO.findByDownloadId() per restituire un Mono.empty()
        when(portFatDownloadDAO.findByDownloadId(anyString())).thenReturn(Mono.empty());
        // Mock di createAndSaveNewDownload() per creare un nuovo PortFatDownload
        when(portFatDownloadDAO.createPortFatDownload(any())).thenReturn(Mono.just(portFatDownload));
        when(portFatDownload.getStatus()).thenReturn(DownloadStatus.COMPLETED);
        when(portFatDownloadDAO.updatePortFatDownload(any())).thenReturn(Mono.just(portFatDownload));
        // Mock di portFatService.processZipFile() per simulare il processo del file
        when(portFatService.processZipFile(any())).thenReturn(Mono.empty());

        // Invoca il metodo pullPortFat
        queueListener.pullPortFat(payload, headers);

        //ASSERT
        // Verifica che il metodo processZipFile sia stato chiamato
        verify(portFatService, times(1)).processZipFile(any());
        // Verifica che lo stato sia stato aggiornato a COMPLETED
        verify(portFatDownloadDAO, times(1)).updatePortFatDownload(portFatDownload);
    }

    @Test
    void testPullPortFatExistingDownloadInErrorState() throws JsonProcessingException {
        //ARRANGE
        headers = new HashMap<>();
        headers.put("AWSTraceHeader", UUID.randomUUID());
        FileReadyModel event = new FileReadyModel();
        event.setFileVersion(FILE_VERSION);
        event.setDownloadUrl(DOWNLOAD_URL);
        event.setFilePath(FILE_PATH);
        payload = new ObjectMapper().writeValueAsString(event);

        // Mock di portFatDownloadDAO.findByDownloadId() per restituire un PortFatDownload in stato ERROR
        when(portFatDownload.getStatus()).thenReturn(DownloadStatus.ERROR);
        when(portFatDownloadDAO.findByDownloadId(anyString())).thenReturn(Mono.just(portFatDownload));
        PortFatDownload updated = mock(PortFatDownload.class);
        when(updated.getStatus()).thenReturn(DownloadStatus.COMPLETED);
        when(portFatDownloadDAO.updatePortFatDownload(any())).thenReturn(Mono.just(updated));

        // Mock di portFatService.processZipFile() per simulare il processo del file
        when(portFatService.processZipFile(any())).thenReturn(Mono.empty());

        // Invoca il metodo pullPortFat
        queueListener.pullPortFat(payload, headers);

        //ASSERT

        // Verifica che il metodo processZipFile sia stato chiamato
        verify(portFatService, times(1)).processZipFile(any());

        // Verifica che lo stato del PortFatDownload sia stato aggiornato a IN_PROGRESS
        verify(portFatDownloadDAO, times(1)).updatePortFatDownload(portFatDownload);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidFileReadyEvents")
    void testPullPortFatWithInvalidFileReadyEvent(String fileVersion, String downloadUrl) throws JsonProcessingException {
        //ARRANGE
        headers = new HashMap<>();
        FileReadyModel event = new FileReadyModel();
        event.setFileVersion(fileVersion);
        event.setDownloadUrl(downloadUrl);
        payload = new ObjectMapper().writeValueAsString(event);

        //ACT
        queueListener.pullPortFat(payload, headers);

        //ASSERT
        verify(portFatService, times(0)).processZipFile(any());
    }

    private static Stream<Arguments> provideInvalidFileReadyEvents() {
        return Stream.of(
                arguments(FILE_VERSION, null), // Invalid: downloadUrl is null
                arguments(FILE_PATH, null), // Invalid: filePath is null
                arguments(FILE_VERSION, "http://esample/"), // Invalid: downloadUrl does not start with base URL
                arguments(FILE_VERSION, "https://portale-fatturazione-storage.blob.core.windows.net/portfatt/IsNodValid/IsNodValid/"), // Invalid path
                arguments(null, DOWNLOAD_URL) // Invalid: fileVersion is null
        );
    }

    @Test
    void testPullPortFatErrorHandling() throws JsonProcessingException {
        //ARRANGE
        headers = new HashMap<>();
        FileReadyModel event = new FileReadyModel();
        event.setFileVersion(FILE_VERSION);
        event.setDownloadUrl(DOWNLOAD_URL);
        event.setFilePath(FILE_PATH);
        payload = new ObjectMapper().writeValueAsString(event);

        // Mock PortFatDownload con stato ERROR
        when(portFatDownloadDAO.findByDownloadId(anyString())).thenReturn(Mono.just(portFatDownload));
        when(portFatDownload.getStatus()).thenReturn(DownloadStatus.ERROR);

        // Mock dell'errore durante il processo
        when(portFatService.processZipFile(any())).thenReturn(Mono.error(new RuntimeException("Processing failed")));
        when(portFatDownloadDAO.updatePortFatDownload(any())).thenReturn(Mono.just(mock(PortFatDownload.class)));

        StepVerifier.create(
                        Mono.fromRunnable(() -> queueListener.pullPortFat(payload, headers))
                )
                .expectError(RuntimeException.class)
                .verify();

        //ASSERT
        // Verifica che lo stato sia stato aggiornato a ERROR
        verify(portFatDownloadDAO, times(2)).updatePortFatDownload(any(PortFatDownload.class));
    }

}
