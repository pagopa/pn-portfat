package it.pagopa.pn.portfat.middleware.msclient.impl;

import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.exception.ExceptionTypeEnum;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.api.FileDownloadApi;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.api.FileUploadApi;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SafeStorageClientImplTest {

    @Mock
    private FileUploadApi fileUploadApi;

    @Mock
    private FileDownloadApi fileDownloadApi;

    @Mock
    private PortFatPropertiesConfig portFatConfig;

    @InjectMocks
    private SafeStorageClientImpl safeStorageClient;

    @Test
    void testCreateFile() {
        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setContentType("application/octet-stream");
        fileCreationRequest.setContent(new byte[]{1, 2, 3});

        String sha256 = "fake-sha256-hash";

        FileCreationResponseDto responseDto = new FileCreationResponseDto();
        responseDto.setKey("mockKey");
        responseDto.setUploadUrl("http://mockupload.url");

        // Definisci cosa deve fare il mock di fileUploadApi quando createFile viene chiamato
        when(fileUploadApi.createFile(anyString(), anyString(), anyString(), any(FileCreationRequestDto.class)))
                .thenReturn(Mono.just(responseDto));

        // Definisci cosa deve fare il mock di portFatConfig
        when(portFatConfig.getSafeStorageCxId()).thenReturn("mockCxId");

        // Esegui il metodo da testare
        Mono<FileCreationResponseDto> result = safeStorageClient.createFile(fileCreationRequest, sha256);

        // Verifica il risultato
        assertNotNull(result);
        FileCreationResponseDto createdFile = result.block();
        assertNotNull(createdFile);
        assertEquals("mockKey", createdFile.getKey());
        assertEquals("http://mockupload.url", createdFile.getUploadUrl());

        // Verifica che il metodo fileUploadApi.createFile è stato chiamato con i parametri corretti
        verify(fileUploadApi, times(1))
                .createFile(("mockCxId"), (sha256), ("SHA-256"), (fileCreationRequest));
    }

    @Test
    void testGetFileSuccess() {
        String fileKey = "fileKey";

        FileDownloadResponseDto response = new FileDownloadResponseDto();
        response.setKey(fileKey);

        when(portFatConfig.getSafeStorageCxId()).thenReturn("mockCxId");

        when(fileDownloadApi.getFile(anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(Mono.just(response));

        Mono<FileDownloadResponseDto> result = safeStorageClient.getFile(fileKey);

        FileDownloadResponseDto dto = result.block();

        assertNotNull(dto);
        assertEquals(fileKey, dto.getKey());

        verify(fileDownloadApi).getFile(fileKey, "mockCxId", false, false);
    }

    @Test
    void testGetFileNotFound() {
        String fileKey = "fileKey";

        when(portFatConfig.getSafeStorageCxId()).thenReturn("mockCxId");

        WebClientResponseException exception = WebClientResponseException.create(
                404,
                "Not Found",
                null,
                null,
                null
        );

        when(fileDownloadApi.getFile(anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(Mono.error(exception));

        PnGenericException ex = assertThrows(PnGenericException.class,
                () -> safeStorageClient.getFile(fileKey).block()
        );

        assertEquals(ExceptionTypeEnum.SAFESTORAGE_GET_FILE_ERROR, ex.getExceptionType());
        assertEquals(
                "File not found in Safe Storage. fileKey=fileKey, status=404 NOT_FOUND",
                ex.getMessage()
        );
    }

    @Test
    void testGetFileGenericError() {
        String fileKey = "fileKey";

        when(portFatConfig.getSafeStorageCxId()).thenReturn("mockCxId");

        WebClientResponseException exception = WebClientResponseException.create(
                500,
                "Internal Server Error",
                null,
                null,
                null
        );

        when(fileDownloadApi.getFile(anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(Mono.error(exception));

        PnGenericException ex = assertThrows(PnGenericException.class,
                () -> safeStorageClient.getFile(fileKey).block()
        );

        assertEquals(ExceptionTypeEnum.SAFESTORAGE_GET_FILE_ERROR, ex.getExceptionType());
        assertEquals(
                "Failed to get file from Safe Storage. fileKey=fileKey, status=500 INTERNAL_SERVER_ERROR",
                ex.getMessage()
        );
    }

}