package it.pagopa.pn.portfat.service;

import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

public interface PortFatService {

    Mono<Void> processZipFile(PortFatDownload portFatDownload);
    Mono<Void> processDirectory(Path directoryPath);
    Path createTmpFile(String prefix, String suffix);
}
