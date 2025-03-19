package it.pagopa.pn.portfat.middleware.msclient.safestorage.impl;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.api.FileUploadApi;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.middleware.msclient.safestorage.SafeStorageClient;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@CustomLog
@Component
@RequiredArgsConstructor
public class SafeStorageClientImpl implements SafeStorageClient {

    private final FileUploadApi fileUploadApi;
    private final PortFatPropertiesConfig portFatConfig;

    @Override
    public Mono<FileCreationResponseDto> createFile(FileCreationWithContentRequest fileCreationRequest, String sha256) {
        final String PN_SAFE_STORAGE_DESCRIPTION = "Safe Storage createFile";
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, PN_SAFE_STORAGE_DESCRIPTION);
        return fileUploadApi.createFile(portFatConfig.getSafeStorageCxId(), sha256, "SHA-256", fileCreationRequest);
    }

}
