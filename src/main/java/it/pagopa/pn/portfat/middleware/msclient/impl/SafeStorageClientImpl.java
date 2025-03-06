package it.pagopa.pn.portfat.middleware.msclient.impl;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.api.FileUploadApi;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.middleware.msclient.SafeStorageClient;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@CustomLog
@Component
@RequiredArgsConstructor
public class SafeStorageClientImpl implements SafeStorageClient {

    private final FileUploadApi fileUploadApi;

    @Override
    public Mono<FileCreationResponseDto> createFile(FileCreationRequestDto fileCreationRequestDto, String safeStorageCxId,  String sha256) {
        final String PN_SAFE_STORAGE_DESCRIPTION = "Safe Storage createFile";
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, PN_SAFE_STORAGE_DESCRIPTION);
       return fileUploadApi.createFile(safeStorageCxId, sha256, "SHA-256",  fileCreationRequestDto);
    }

}
