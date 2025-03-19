package it.pagopa.pn.portfat.service;

import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import reactor.core.publisher.Mono;

public interface PortFatService {

    Mono<Void> processZipFile(PortFatDownload portFatDownload);
}
