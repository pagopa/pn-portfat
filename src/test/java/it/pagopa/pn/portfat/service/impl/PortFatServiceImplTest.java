package it.pagopa.pn.portfat.service.impl;

import it.pagopa.pn.portfat.config.HttpConnectorWebClient;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.exception.PnGenericException;
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
        when(portFatConfig.getZipExtension()).thenReturn(".zip");
        mockedUtility.when(() -> Utility.computeSHA256(any(Path.class)))
                .thenReturn("98ISBVOIAHDBVIHBSVIHB");

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
        when(portFatConfig.getZipExtension()).thenReturn(".zip");
        when(webClient.downloadFileAsByteArray(anyString(), any(Path.class)))
                .thenReturn(Mono.error(new RuntimeException("Error downloading file")));

        // Act - Assert
        StepVerifier.create(portFatService.processZipFile(portFatDownload))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Error downloading file"))
                .verify();

        // Verify no interactions
        verify(safeStorageService, never()).createAndUploadContent(any());
        verify(portFatDownloadDAO, never()).updatePortFatDownload(any());
    }

    @Test
    void testProcessZipFile_Failure_SHA256Calculation() {
        when(portFatConfig.getZipExtension()).thenReturn(".zip");

        when(webClient.downloadFileAsByteArray(anyString(), any(Path.class)))
                .thenReturn(Mono.empty());

        mockedUtility.when(() -> Utility.computeSHA256(any(Path.class)))
                .thenThrow(new RuntimeException("Error calculating SHA-256"));

        StepVerifier.create(portFatService.processZipFile(portFatDownload))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Error calculating SHA-256"))
                .verify();

        // Verify no interactions
        verify(safeStorageService, never()).createAndUploadContent(any());
        verify(portFatDownloadDAO, never()).updatePortFatDownload(any());
    }


    @Test
    void testProcessDirectorySuccess() throws Exception {
        Path tempDir = Files.createTempDirectory("test-dir");
        Files.createFile(tempDir.resolve("file.json"));

        mockedUtility.when(() -> Utility.convertToObject(any(File.class), any()))
                .thenReturn(new PortaleFatturazioneModel());

        mockedUtility.when(() -> Utility.jsonToByteArray(any()))
                .thenReturn("{}".getBytes());

        when(safeStorageService.createAndUploadContent(any()))
                .thenReturn(Mono.just("OK"));

        StepVerifier.create(portFatService.processDirectory(tempDir, "fileKey", false))
                .verifyComplete();

        verify(safeStorageService, atLeastOnce())
                .createAndUploadContent(any());
    }

    @Test
    void testProcessDirectoryFailure_ProcessFileError() throws Exception {
        Path tempDir = Files.createTempDirectory("test-dir");
        Files.createFile(tempDir.resolve("file.json"));

        mockedUtility.when(() -> Utility.convertToObject(any(File.class), any()))
                .thenThrow(new RuntimeException("parse error"));

        StepVerifier.create(portFatService.processDirectory(tempDir, "fileKey", false))
                .expectError(RuntimeException.class)
                .verify();

        verifyNoInteractions(safeStorageService);
    }

    @Test
    void testProcessDirectoryFailure() {
        Path invalidPath = Path.of("not-existing-dir");

        StepVerifier.create(portFatService.processDirectory(invalidPath, "fileKey", false))
                .expectError(PnGenericException.class)
                .verify();
    }
}