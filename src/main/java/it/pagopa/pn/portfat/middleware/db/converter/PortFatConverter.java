package it.pagopa.pn.portfat.middleware.db.converter;

import it.pagopa.pn.portfat.middleware.db.entities.DownloadStatus;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.model.FileReadyModel;

import java.time.Instant;

import static it.pagopa.pn.portfat.utils.Utility.downloadId;

/**
 * Classe di utilità per la gestione delle conversioni dei dati relativi a Portale Fatturazione.
 * <p>
 * Fornisce metodi statici per aggiornare lo stato dei download e per la creazione di istanze di PortFatDownload.
 */
public class PortFatConverter {

    /**
     * Costruttore privato per impedire l'istanza della classe.
     * <p>
     * Lancia un'eccezione se chiamato.
     */
    private PortFatConverter() {
        throw new IllegalCallerException();
    }

    /**
     * Segna un download come completato, resettando eventuali messaggi di errore e aggiornando il timestamp.
     *
     * @param portFatDownload l'istanza di PortFatDownload da aggiornare
     */
    public static void completed(PortFatDownload portFatDownload) {
        portFatDownload.setStatus(DownloadStatus.COMPLETED);
        portFatDownload.setErrorMessage(null);
        portFatDownload.setUpdatedAt(Instant.now().toString());
    }

    /**
     * Aggiorna lo stato di un download e aggiorna il timestamp dell'ultimo aggiornamento.
     *
     * @param portFatDownload l'istanza di PortFatDownload da aggiornare
     * @param status          il nuovo stato da impostare
     */
    public static void portFatDownload(PortFatDownload portFatDownload, DownloadStatus status) {
        portFatDownload.setUpdatedAt(Instant.now().toString());
        portFatDownload.setStatus(status);
    }

    /**
     * Crea un'istanza di PortFatDownload a partire da un evento FileReadyModel.
     *
     * @param fileReadyEvent l'evento contenente i dati del file pronto per il download
     * @return una nuova istanza di PortFatDownload inizializzata con i dati dell'evento
     */
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
