package it.pagopa.pn.portfat.middleware.msclient.safestorage.impl;

import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.api.FileUploadApi;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SafeStorageClientImplTest {

    @Mock
    private FileUploadApi fileUploadApi;

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

}