package it.pagopa.pn.portfat.config;

import reactor.core.publisher.Mono;

import java.nio.file.Path;

/**
 * HTTP reactive client used to invoke external services that do not have OpenAPI documentation, such as the S3 service
 */
public interface HttpConnector {

    Mono<Void> downloadFileAsByteArray(String url, Path fileOutput);

}
