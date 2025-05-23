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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.LIST_FILES_ERROR;
import static it.pagopa.pn.portfat.mapper.FileCreationWithContentRequestMapper.mapper;
import static it.pagopa.pn.portfat.utils.Utility.*;
import static it.pagopa.pn.portfat.utils.ZipUtility.unzip;

/**
 * Implementazione del servizio per la gestione dei file Portale Fatturazione.
 * <p>
 * Questa classe fornisce metodi per il download, l'elaborazione e lo storage sicuro dei file ZIP ricevuti.
 */
@Service
@CustomLog
@AllArgsConstructor
public class PortFatServiceImpl implements PortFatService {

    private static final String TIME_FORMAT = "yyyy-MM-dd_HH-mm-ss-SSS";
    private static final String PATH_FIELS = "port-fat-files";

    private final PortFatPropertiesConfig portFatConfig;
    private final HttpConnectorWebClient webClient;
    private final PortFatDownloadDAO portFatDownloadDAO;
    private final SafeStorageService safeStorageService;

    // TODO ORIGINAL_DATA_UPDATE in FileCreationWithContentRequestMapper

    /**
     * Crea le directory
     * Scarica il file zip da portale fatturazione
     * Crea un SHA-256 del file ZIP, aggiorno a db l'entità con lo sha creato
     * Estrae e processa i file contenuti nello ZIP
     * La directory viene eliminata indifferentemente se il processo si terminato con successo o sia andato in errore.
     *
     * @param portFatDownload l'oggetto contenente le informazioni sul file da scaricare
     * @return un Mono che completa l'elaborazione del file ZIP
     */
    @Override
    public Mono<Void> processZipFile(PortFatDownload portFatDownload) {
        log.info("processZipFile,  downloadUrl {}, DOWNLOAD_ID={}", portFatDownload.getDownloadUrl(), portFatDownload.getDownloadId());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT));
        Path outputPath = Path.of(portFatConfig.getBasePathZipFile(), timestamp);
        Path outputFilesPath = Path.of(outputPath.toString(), PATH_FIELS);
        String fileName = UUID.randomUUID().toString();
        Path zipFilePath = outputPath.resolve(fileName + portFatConfig.getZipExtension());
        return Mono.defer(() -> createDirectories(outputPath)
                        .then(createDirectories(outputFilesPath))
                        .then(webClient.downloadFileAsByteArray(portFatDownload.getDownloadUrl(), zipFilePath))
                )
                .then(Mono.fromCallable(() -> computeSHA256(zipFilePath)))
                .doOnNext(hash -> {
                    log.info("SHA-256 Hash: {}", hash);
                    portFatDownload.setSha256(hash);
                })
                .flatMap(hash -> portFatDownloadDAO.updatePortFatDownload(portFatDownload))
                .then(unzip(zipFilePath.toString(), outputFilesPath.toString()))
                .then(Mono.fromRunnable(() -> {
                    try {
                        Files.deleteIfExists(zipFilePath);
                    } catch (IOException e) {
                        log.error("Errore nell'eliminazione dello ZIP: {}", zipFilePath, e);
                    }
                }))
                .thenMany(processDirectory(outputFilesPath))
                .then()
                .doFinally(signal -> deleteFileOrDirectory(outputPath.toFile()));
    }

    /**
     * Processa i file all'interno di una directory, elaborandoli singolarmente.
     *
     * @param directoryPath il percorso della directory contenente i file json
     * @return un Mono che completa l'elaborazione dei file presenti nella directory
     */
    public Mono<Void> processDirectory(Path directoryPath) {
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
                    return processFile(file, parentDirectory.getFileName().toString());
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
    private Mono<Void> processFile(Path file, String parentDirectoryName) {
        log.info("Processing file: {} in folder: {}", file.getFileName(), parentDirectoryName);
        return Mono.fromCallable(() -> convertToObject(file.toFile(), PortaleFatturazioneModel.class))
                .flatMap(portaleFatturazioneModel ->
                        Mono.just(jsonToByteArray(portaleFatturazioneModel))
                                .flatMap(jsonToByteArray -> {
                                    FileCreationWithContentRequest fileCreationRequest = mapper(jsonToByteArray, portaleFatturazioneModel);
                                    return safeStorageService.createAndUploadContent(fileCreationRequest);
                                })
                )
                .onErrorResume(e -> {
                    log.error("Error processing file: {}", file, e);
                    return Mono.error(e);
                }).then();
    }
}
