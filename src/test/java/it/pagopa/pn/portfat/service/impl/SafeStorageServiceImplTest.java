package it.pagopa.pn.portfat.service.impl;

import it.pagopa.pn.portfat.middleware.msclient.webclient.HttpConnectorWebClient;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.middleware.msclient.safestorage.SafeStorageClient;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SafeStorageServiceImplTest {

    @InjectMocks
    private SafeStorageServiceImpl safeStorageService;

    @Mock
    private SafeStorageClient safeStorageClient;

    @Mock
    private HttpConnectorWebClient httpConnectorWebClient;

    @Test
    void testCreateAndUploadContent() {
        // Prepara i dati per il test
        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setDocumentType("pdf");
        fileCreationRequest.setContent("fake content".getBytes());

        String sha256 = "mLGuRQWbAEF4qO7gwfYXnc6hOcD9imnuR6bwLZevHxc=";

        FileCreationResponseDto fileCreationResponseDto = new FileCreationResponseDto();
        fileCreationResponseDto.setKey("mockKey");
        fileCreationResponseDto.setUploadUrl("http://mockupload.url");

        // Definisci cosa deve fare il mock di safeStorageClient quando createFile viene chiamato
        when(safeStorageClient.createFile(any(), any())).thenReturn(Mono.just(fileCreationResponseDto));

        when(httpConnectorWebClient.uploadContent(any(), any(), any())).thenReturn(Mono.empty());

        // Esegui il metodo da testare
        Mono<String> result = safeStorageService.createAndUploadContent(fileCreationRequest);

        // Verifica il risultato
        assertNotNull(result);
        String resultKey = result.block();
        assertEquals("mockKey", resultKey);

        // Verifica che i metodi mockati siano stati chiamati
        verify(safeStorageClient, times(1))
                .createFile(fileCreationRequest, sha256);
        verify(httpConnectorWebClient, times(1))
                .uploadContent(fileCreationRequest, fileCreationResponseDto, sha256);
    }

    @Test
    void testCreateAndUploadContentThrowsPnGenericException() {
        // Prepara i dati per il test
        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setDocumentType("pdf");
        fileCreationRequest.setContent("fake content".getBytes());
        String sha256 = "mLGuRQWbAEF4qO7gwfYXnc6hOcD9imnuR6bwLZevHxc=";

        // Crea una risposta errore per safeStorageClient
        when(safeStorageClient.createFile(any(), any()))
                .thenReturn(Mono.error(new RuntimeException("Error creating file")));

        // Esegui il metodo da testare e verifica che venga lanciata l'eccezione
        Mono<String> result = safeStorageService.createAndUploadContent(fileCreationRequest);

        // Verifica che l'eccezione venga sollevata
        PnGenericException exception = assertThrows(PnGenericException.class, result::block);
        assertEquals("Error in file creation flow, save storage: Error creating file", exception.getMessage());

        // Verifica che i metodi siano stati chiamati con gli argomenti corretti
        verify(safeStorageClient, times(1)).createFile(fileCreationRequest, sha256);
    }

    @Test
    void shouldReturnErrorWhenSafeStorageClientFails() {
        when(safeStorageClient.createFile(any(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Storage error")));
        byte[] content = "Test content".getBytes();

        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setContent(content);
        fileCreationRequest.setDocumentType("DOC_TYPE");

        StepVerifier.create(safeStorageService.createAndUploadContent(fileCreationRequest))
                .expectErrorMatches(e -> e instanceof PnGenericException &&
                        e.getMessage().contains("Storage error"))
                .verify();

        verify(safeStorageClient, times(1)).createFile(any(), anyString());
        verifyNoInteractions(httpConnectorWebClient);
    }

}