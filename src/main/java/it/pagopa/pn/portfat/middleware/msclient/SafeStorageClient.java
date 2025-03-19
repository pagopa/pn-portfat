package it.pagopa.pn.portfat.middleware.msclient;

import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import reactor.core.publisher.Mono;

public interface SafeStorageClient {

    Mono<FileCreationResponseDto> createFile(FileCreationWithContentRequest fileCreationRequestWithContent, String sha256);

}
