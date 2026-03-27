package it.pagopa.pn.portfat.service.impl;

import it.pagopa.pn.portfat.config.HttpConnectorWebClient;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import it.pagopa.pn.portfat.model.PortaleFatturazioneModel;
import it.pagopa.pn.portfat.service.PortFatService;
import it.pagopa.pn.portfat.service.SafeStorageService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.LIST_FILES_ERROR;
import static it.pagopa.pn.portfat.mapper.FileCreationWithContentRequestMapper.mapper;
import static it.pagopa.pn.portfat.utils.Utility.*;

/**
 * Implementazione del servizio per la gestione dei file Portale Fatturazione.
 * <p>
 * Questa classe fornisce metodi per il download, l'elaborazione e lo storage sicuro dei file ZIP ricevuti.
 */
@Service
@CustomLog
@AllArgsConstructor
public class PortFatServiceImpl implements PortFatService {

    static final String PN_SERVICE_ORDER = "PN_SERVICE_ORDER";
    static final String PN_SERVICE_ORDER_ARCHIVE = "PN_SERVICE_ORDER_ARCHIVE";
    public static final String ARCHIVE_PROCESSED_AT = "archiveProcessedAt";

    private final PortFatPropertiesConfig portFatConfig;
    private final HttpConnectorWebClient webClient;
    private final PortFatDownloadDAO portFatDownloadDAO;
    private final SafeStorageService safeStorageService;

    // TODO ORIGINAL_DATA_UPDATE in FileCreationWithContentRequestMapper

    /**
     * Scarica un file ZIP da Portale Fatturazione e lo carica su Safe Storage.
     *
     * <p>Flusso operativo:
     * <ul>
     *   <li>Scarica il file ZIP in una directory temporanea</li>
     *   <li>Calcola l'hash SHA-256 del file e lo imposta sull'entità</li>
     *   <li>Legge il contenuto del file come array di byte</li>
     *   <li>Crea la richiesta di upload e invia il file al Safe Storage</li>
     *   <li>Aggiorna l'entità con la chiave del file archiviato</li>
     *   <li>Elimina il file temporaneo al termine del processo</li>
     * </ul>
     *
     * <p>Il file temporaneo viene eliminato al termine del flusso.
     *
     * @param portFatDownload oggetto contenente le informazioni sul file da scaricare
     * @return un Mono che completa l'elaborazione del file ZIP
     */
    @Override
    public Mono<Void> processZipFile(PortFatDownload portFatDownload) {
        log.info("processZipFile,  downloadUrl {}, DOWNLOAD_ID={}", portFatDownload.getDownloadUrl(), portFatDownload.getDownloadId());
        String fileName = UUID.randomUUID().toString();
        Path zipFilePath = createTmpFile(fileName, portFatConfig.getZipExtension());
        return webClient.downloadFileAsByteArray(portFatDownload.getDownloadUrl(), zipFilePath)
                .then(Mono.fromCallable(() -> computeSHA256(zipFilePath)))
                .doOnNext(portFatDownload::setSha256)
                .flatMap(ignored ->
                        Mono.fromCallable(() -> Files.readAllBytes(zipFilePath))
                )
                .flatMap(fileBytes -> {
                    FileCreationWithContentRequest request = mapper(fileBytes, MediaType.APPLICATION_OCTET_STREAM_VALUE, PN_SERVICE_ORDER_ARCHIVE);
                    return safeStorageService.createAndUploadContent(request);
                })
                .flatMap(archiveFileKey -> {
                    portFatDownload.setArchiveFileKey(archiveFileKey);
                    return portFatDownloadDAO.updatePortFatDownload(portFatDownload);
                })
                .doFinally(signalType -> {
                    try {
                        Files.deleteIfExists(zipFilePath);
                        log.debug("Temp ZIP deleted: {}", zipFilePath);
                    } catch (IOException e) {
                        log.error("Errore nell'eliminazione dello ZIP: {}", zipFilePath, e);
                    }
                })
                .then();
    }

    public Path createTmpFile(String prefix, String suffix) {
        try {
            return Files.createTempFile("tmp_" + prefix, suffix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Processa i file all'interno di una directory, elaborandoli singolarmente.
     *
     * @param directoryPath il percorso della directory contenente i file json
     * @return un Mono che completa l'elaborazione dei file presenti nella directory
     */
    public Mono<Void> processDirectory(Path directoryPath) {
        Instant archiveProcessedAt = Instant.now();
        return Flux.fromStream(() -> {
                    try {
                        return Files.walk(directoryPath);
                    } catch (IOException e) {
                        throw new PnGenericException(LIST_FILES_ERROR, LIST_FILES_ERROR.getMessage() + e.getMessage());
                    }
                })
                .filter(Files::isRegularFile)
                .flatMap(file -> {
                    Path parentDirectory = file.getParent();
                    return processFile(file, parentDirectory.getFileName().toString(), archiveProcessedAt);
                }, 10)
                .then();
    }

    /**
     * Processa un singolo file all'interno della directory,
     * convertendolo in un oggetto di dominio e caricandolo in un safe storage.
     *
     * @param file                il file da processare
     * @param parentDirectoryName il nome della directory di appartenenza del file
     * @return un Mono che completa l'elaborazione del file
     */
    private Mono<Void> processFile(Path file, String parentDirectoryName, Instant archiveProcessedAt) {
        log.info("Processing file: {} in folder: {}", file.getFileName(), parentDirectoryName);
        return Mono.fromCallable(() -> convertToObject(file.toFile(), PortaleFatturazioneModel.class))
                .flatMap(portaleFatturazioneModel ->
                        Mono.just(jsonToByteArray(portaleFatturazioneModel))
                                .flatMap(jsonToByteArray -> {
                                    FileCreationWithContentRequest fileCreationRequest = mapper(jsonToByteArray, MediaType.APPLICATION_JSON_VALUE, PN_SERVICE_ORDER);
                                    fileCreationRequest.setTags(Map.of(ARCHIVE_PROCESSED_AT, List.of(archiveProcessedAt.toString())));
                                    return safeStorageService.createAndUploadContent(fileCreationRequest);
                                })
                )
                .onErrorResume(e -> {
                    log.error("Error processing file: {}", file, e);
                    return Mono.error(e);
                }).then();
    }
}
