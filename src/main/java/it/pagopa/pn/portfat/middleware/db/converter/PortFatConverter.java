package it.pagopa.pn.portfat.middleware.db.converter;

import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import it.pagopa.pn.portfat.middleware.db.entities.DownloadStatus;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import static it.pagopa.pn.portfat.utils.Utility.downloadId;

import java.time.Instant;

public class PortFatConverter {

    private PortFatConverter() {
        throw new IllegalCallerException();
    }

    public static PortFatDownload portFatDownloadToEntity(FileReadyEvent fileReadyEvent) {
        return PortFatDownload.builder()
                .downloadId(downloadId(fileReadyEvent))
                .downloadUrl(fileReadyEvent.getDownloadUrl())
                .fileVersion(fileReadyEvent.getFileVersion())
                .status(DownloadStatus.IN_PROGRESS)
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .build();
    }
}
