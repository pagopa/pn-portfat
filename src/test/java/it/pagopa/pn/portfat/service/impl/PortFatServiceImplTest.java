package it.pagopa.pn.portfat.service.impl;

import it.pagopa.pn.portfat.config.HttpConnectorWebClient;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.model.PortaleFatturazioneModel;
import it.pagopa.pn.portfat.service.SafeStorageService;
import it.pagopa.pn.portfat.utils.Utility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PortFatServiceImplTest {

    @InjectMocks
    private PortFatServiceImpl portFatService;

    @Mock
    private PortFatPropertiesConfig portFatConfig;

    @Mock
    private HttpConnectorWebClient webClient;

    @Mock
    private PortFatDownloadDAO portFatDownloadDAO;

    @Mock
    private SafeStorageService safeStorageService;

    private MockedStatic<Utility> mockedUtility;

    private PortFatDownload portFatDownload;

    @BeforeEach
    void setUp() {
        portFatDownload = new PortFatDownload();
        String downloadUrl = "http://example.com/test.zip";
        portFatDownload.setDownloadUrl(downloadUrl);
        when(portFatConfig.getBasePathZipFile()).thenReturn("target/test-zip");
        when(portFatConfig.getZipExtension()).thenReturn(".zip");
        mockedUtility = Mockito.mockStatic(Utility.class);
    }

    @AfterEach
    void tearDown() {
        mockedUtility.close();
    }

    @Test
    void testProcessZipFile_Success() {
        // Arrange
        ClassLoader classLoader = getClass().getClassLoader();
        PortaleFatturazioneModel portaleFatturazioneModel = new PortaleFatturazioneModel();
        portaleFatturazioneModel.setFkIdEnte("45GRTF");
        portaleFatturazioneModel.setAnnoValidita(2025);
        portaleFatturazioneModel.setMeseValidita(3);

        mockedUtility.when(() -> Utility.createDirectories(any(Path.class)))
                .thenReturn(Mono.just(true).then());
        mockedUtility.when(() -> Utility.computeSHA256(any(Path.class)))
                .thenReturn("98ISBVOIAHDBVIHBSVIHB");
        mockedUtility.when(() -> Utility.deleteFileOrDirectory(any(File.class)))
                .thenReturn(Mono.just(true).then());
        mockedUtility.when(() -> Utility.convertToObject(any(File.class), eq(PortaleFatturazioneModel.class)))
                .thenReturn(portaleFatturazioneModel);
        mockedUtility.when(() -> Utility.jsonToByteArray(any()))
                .thenReturn(new byte[]{1, 2, 3});

        when(webClient.downloadFileAsByteArray(anyString(), any()))
                .thenAnswer(invocation -> {
                    Path zipFilePath = invocation.getArgument(1);
                    Files.createDirectories(zipFilePath.getParent());
                    InputStream inputStream = classLoader.getResourceAsStream("portFatt.zip");
                    if (inputStream == null) {
                        throw new FileNotFoundException("File portFatt.zip non trovato in resources");
                    }
                    Files.copy(inputStream, zipFilePath, StandardCopyOption.REPLACE_EXISTING);
                    return Mono.just(new byte[]{1, 2, 3});
                });

        when(portFatDownloadDAO.updatePortFatDownload(any()))
                .thenReturn(Mono.just(portFatDownload));
        when(safeStorageService.createAndUploadContent(any()))
                .thenReturn(Mono.just("TRDTYO"));

        StepVerifier.create(portFatService.processZipFile(portFatDownload))
                .expectSubscription()
                .verifyComplete();

        // Act - Assert
        verify(webClient, times(1)).downloadFileAsByteArray(anyString(), any());
        verify(portFatDownloadDAO, times(1)).updatePortFatDownload(any());
        verify(safeStorageService, times(1)).createAndUploadContent(any());
    }

    @Test
    void testProcessZipFile_Failure_DownloadFile() {
        // Arrange
        when(webClient.downloadFileAsByteArray(anyString(), any(Path.class)))
                .thenReturn(Mono.error(new RuntimeException("Error downloading file")));
        mockedUtility.when(() -> Utility.createDirectories(any(Path.class)))
                .thenReturn(Mono.just(true).then());
        mockedUtility.when(() -> Utility.deleteFileOrDirectory(any(File.class)))
                .thenReturn(Mono.just(true).then());

        // Act - Assert
        StepVerifier.create(portFatService.processZipFile(portFatDownload))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Error downloading file"))
                .verify();
    }

    @Test
    void testProcessZipFile_Failure_SHA256Calculation() {

        mockedUtility.when(() -> Utility.createDirectories(any(Path.class)))
                .thenReturn(Mono.just(true).then());
        mockedUtility.when(() -> Utility.deleteFileOrDirectory(any(File.class)))
                .thenReturn(Mono.just(true).then());

        when(webClient.downloadFileAsByteArray(anyString(), any(Path.class)))
                .thenReturn(Mono.empty());

        mockedUtility.when(() -> Utility.computeSHA256(any(Path.class)))
                .thenThrow(new RuntimeException("Error calculating SHA-256"));

        StepVerifier.create(portFatService.processZipFile(portFatDownload))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Error calculating SHA-256"))
                .verify();
    }

}