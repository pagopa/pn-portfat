package it.pagopa.pn.portfat.middleware.queue;

import it.pagopa.pn.portfat.config.BaseTest;
import it.pagopa.pn.portfat.config.HttpConnectorWebClient;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.exception.PnPortfatDownloadNotFoundException;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.service.PortFatService;
import it.pagopa.pn.portfat.service.SafeStorageService;
import it.pagopa.pn.portfat.utils.Utility;
import it.pagopa.pn.portfat.utils.ZipUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class SafeStorageToPortfatQueueListenerTest extends BaseTest.WithLocalStack {

    @Autowired
    private SafeStorageToPortfatQueueListener listener;

    @MockitoBean
    private PortFatPropertiesConfig portFatConfig;

    @MockitoBean
    private PortFatDownloadDAO portFatDownloadDAO;

    @MockitoBean
    private SafeStorageService safeStorageService;

    @MockitoBean
    private PortFatService portFatService;

    @MockitoBean
    private HttpConnectorWebClient webClient;

    private MockedStatic<Utility> utilityMock;
    private MockedStatic<ZipUtility> zipMock;

    private FileDownloadResponseDto payload;
    private Map<String, Object> headers;
    private PortFatDownload portFatDownload;

    private static final String FILE_KEY = "fileKey";

    @BeforeEach
    void setUp() {
        utilityMock = mockStatic(Utility.class);
        zipMock = mockStatic(ZipUtility.class);

        payload = new FileDownloadResponseDto();
        payload.setKey(FILE_KEY);

        headers = new HashMap<>();
        headers.put("id", "test-id");

        portFatDownload = new PortFatDownload();

        when(portFatConfig.getBasePathZipFile()).thenReturn("test");
        when(portFatConfig.getZipExtension()).thenReturn(".zip");

        when(portFatService.createTmpFile(anyString(), anyString()))
                .thenReturn(Path.of("test.zip"));

        utilityMock.when(() -> Utility.createDirectories(anyString()))
                .thenReturn(Path.of("test-dir"));

        zipMock.when(() -> ZipUtility.unzip(anyString(), anyString()))
                .thenReturn(Mono.just(true));
    }

    @AfterEach
    void tearDown() {
        utilityMock.close();
        zipMock.close();
    }

    @Test
    void testHappyPath() {

        when(portFatDownloadDAO.findByArchiveFileKey(FILE_KEY))
                .thenReturn(Mono.just(portFatDownload));

        when(portFatDownloadDAO.updatePortFatDownload(any()))
                .thenReturn(Mono.just(portFatDownload));

        when(safeStorageService.callSafeStorageGetFile(FILE_KEY))
                .thenReturn(Mono.just("url"));

        when(webClient.downloadFileAsByteArray(anyString(), any()))
                .thenReturn(Mono.empty());

        when(portFatService.processDirectory(any()))
                .thenReturn(Mono.empty());

        listener.safeStorageToPortfatConsumer(payload, headers);

        verify(portFatService).processDirectory(any());
        verify(portFatDownloadDAO).updatePortFatDownload(any());
    }

    @Test
    void testDownloadNotFound() {

        when(portFatDownloadDAO.findByArchiveFileKey(FILE_KEY))
                .thenReturn(Mono.empty());

        assertThrows(PnPortfatDownloadNotFoundException.class, () ->
                listener.safeStorageToPortfatConsumer(payload, headers)
        );

        verify(portFatDownloadDAO, times(1)).findByArchiveFileKey(FILE_KEY);
        verify(portFatDownloadDAO, never()).updatePortFatDownload(any());
    }

    @Test
    void testErrorDuringProcessing() {

        when(portFatDownloadDAO.findByArchiveFileKey(FILE_KEY))
                .thenReturn(Mono.just(portFatDownload));

        when(safeStorageService.callSafeStorageGetFile(FILE_KEY))
                .thenReturn(Mono.error(new RuntimeException("Processing failed")));

        when(portFatDownloadDAO.updatePortFatDownload(any()))
                .thenReturn(Mono.just(portFatDownload));

        listener.safeStorageToPortfatConsumer(payload, headers);

        verify(portFatDownloadDAO, times(2)).findByArchiveFileKey(FILE_KEY);
        verify(portFatDownloadDAO, times(1)).updatePortFatDownload(any());    }
}