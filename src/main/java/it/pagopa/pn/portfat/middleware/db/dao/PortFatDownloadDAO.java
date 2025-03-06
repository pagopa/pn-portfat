package it.pagopa.pn.portfat.middleware.db.dao;

import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import reactor.core.publisher.Mono;

public interface PortFatDownloadDAO {

    Mono<PortFatDownload> findByDownloadId(String downloadId);

    Mono<PortFatDownload> createPortFatDownload(PortFatDownload pnAddress);

    Mono<PortFatDownload> updatePortFatDownload(PortFatDownload pnAddress);

}
