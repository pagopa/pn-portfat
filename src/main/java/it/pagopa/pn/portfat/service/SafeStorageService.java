package it.pagopa.pn.portfat.service;

import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import reactor.core.publisher.Mono;

public interface SafeStorageService {

    Mono<String> createAndUploadContent(FileCreationWithContentRequest fileCreationRequest, String sha256);
}
