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
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Mono;
import java.security.MessageDigest;
import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.CREATION_FILE_SS_ERROR;
import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.SHA256_ERROR;


@Component
@Slf4j
@RequiredArgsConstructor
public class SafeStorageServiceImpl implements SafeStorageService {

    private final SafeStorageClient safeStorageClient;
    private final HttpConnector httpConnector;

    @Override
    public Mono<String> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest) {
        log.info("Start createAndUploadFile - documentType={} fileSize={}", fileCreationRequest.getDocumentType(), fileCreationRequest.getContent().length);
        String sha256 = computeSha256(fileCreationRequest.getContent());
        return safeStorageClient.createFile(fileCreationRequest, sha256)
                .onErrorResume(exception -> {
                    log.error("Cannot create file ", exception);
                    return Mono.error(new PnGenericException(CREATION_FILE_SS_ERROR, CREATION_FILE_SS_ERROR.getMessage() + exception.getMessage()));
                })
                .flatMap(fileCreationResponse -> httpConnector.uploadContent(fileCreationRequest, fileCreationResponse, sha256)
                        .thenReturn(fileCreationResponse))
                .map(FileCreationResponseDto::getKey)
                .doOnNext(s -> log.info("End createAndUploadFile - {}", s));
    }

    private static String computeSha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(content);
            return bytesToBase64(encodedHash);
        } catch (Exception exc) {
            throw new PnGenericException(SHA256_ERROR, exc.getMessage());
        }
    }

    private static String bytesToBase64(byte[] hash) {
        return Base64Utils.encodeToString(hash);
    }
}
