package it.pagopa.pn.portfat.middleware.db.converter;

import it.pagopa.pn.portfat.middleware.db.entities.DownloadStatus;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.model.FileReadyModel;
import static it.pagopa.pn.portfat.utils.Utility.downloadId;
import java.time.Instant;


public class PortFatConverter {

    private PortFatConverter() {
        throw new IllegalCallerException();
    }

    public static void completed(PortFatDownload portFatDownload) {
        portFatDownload.setStatus(DownloadStatus.COMPLETED);
        portFatDownload.setErrorMessage(null);
        portFatDownload.setUpdatedAt(Instant.now().toString());
    }

    public static void portFatDownload(PortFatDownload portFatDownload, DownloadStatus status) {
        portFatDownload.setUpdatedAt(Instant.now().toString());
        portFatDownload.setStatus(status);
    }

    public static PortFatDownload portFatDownload(FileReadyModel fileReadyEvent) {
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
