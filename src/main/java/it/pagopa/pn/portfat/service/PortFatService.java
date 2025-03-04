package it.pagopa.pn.portfat.service;

import reactor.core.publisher.Mono;

public interface PortFatService {

    Mono<Void> processZipFile(String url);
}
