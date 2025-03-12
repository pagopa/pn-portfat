package it.pagopa.pn.portfat.config;

import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

/**
 * HTTP reactive client used to invoke external services that do not have OpenAPI documentation, such as the S3 service
 */
public interface HttpConnector {

    Mono<Void> downloadFileAsByteArray(String url, Path fileOutput);

    Mono<Void> uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponseDto fileCreationResponse, String sha256);

}
