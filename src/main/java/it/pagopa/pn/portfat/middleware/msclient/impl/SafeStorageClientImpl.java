package it.pagopa.pn.portfat.middleware.msclient.impl;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.exception.ExceptionTypeEnum;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.api.FileDownloadApi;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.api.FileUploadApi;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.portfat.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Implementazione del client per l'interazione con il servizio di Safe Storage.
 * Questa classe gestisce la creazione di file e l'invocazione delle API di Safe Storage.
 */
@CustomLog
@Component
@RequiredArgsConstructor
public class SafeStorageClientImpl implements SafeStorageClient {

    private final FileUploadApi fileUploadApi;
    private final FileDownloadApi fileDownloadApi;
    private final PortFatPropertiesConfig portFatConfig;

    /**
     * Crea un nuovo file su Safe Storage utilizzando i dati forniti nella richiesta.
     *
     * @param fileCreationRequest l'oggetto contenente il contenuto e i metadati del file da creare
     * @param sha256              l'hash SHA-256 del file per la verifica dell'integrità
     * @return un {@code Mono<FileCreationResponseDto>} contenente la risposta della creazione del file
     */
    @Override
    public Mono<FileCreationResponseDto> createFile(FileCreationWithContentRequest fileCreationRequest, String sha256) {
        final String PN_SAFE_STORAGE_DESCRIPTION = "Safe Storage createFile";
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, PN_SAFE_STORAGE_DESCRIPTION);
        return fileUploadApi.createFile(portFatConfig.getSafeStorageCxId(), sha256, "SHA-256", fileCreationRequest);
    }

    @Override
    public Mono<FileDownloadResponseDto> getFile(String fileKey) {
        log.debug("Req params : {}", fileKey);
        return fileDownloadApi.getFile(fileKey, this.portFatConfig.getSafeStorageCxId(), false, false)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error(ex.getResponseBodyAsString());
                    String errorMessage;
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        errorMessage = String.format("File not found in Safe Storage. fileKey=%s, status=%s", fileKey, ex.getStatusCode());
                    } else {
                        errorMessage = String.format("Failed to get file from Safe Storage. fileKey=%s, status=%s", fileKey, ex.getStatusCode());
                    }
                    return Mono.error(new PnGenericException(ExceptionTypeEnum.ZIP_ERROR, errorMessage));
                });
    }

}
