package it.pagopa.pn.portfat.service.impl;

import it.pagopa.pn.portfat.config.HttpConnector;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import it.pagopa.pn.portfat.service.SafeStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.CREATION_FILE_SS_ERROR;

@Component
@Slf4j
@RequiredArgsConstructor
public class SafeStorageServiceImpl implements SafeStorageService {

    private final SafeStorageClient safeStorageClient;
    private final HttpConnector httpConnector;

    @Override
    public Mono<String> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest, String sha256) {
        log.info("Start createAndUploadFile - documentType={} fileSize={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length);

        return safeStorageClient.createFile(fileCreationRequest, sha256)
                .onErrorResume(exception -> {
                    log.error("Cannot create file ", exception);
                    return Mono.error(new PnGenericException(CREATION_FILE_SS_ERROR, CREATION_FILE_SS_ERROR.getMessage() + exception.getMessage()));
                })
                .flatMap(fileCreationResponse -> httpConnector.uploadContent(fileCreationRequest, fileCreationResponse, sha256)
                        .thenReturn(fileCreationResponse))
                .map(FileCreationResponseDto::getKey);
    }
}
