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

    @Override
    public Mono<Void> processZipFile(PortFatDownload portFatDownload) {
        log.info("processZipFile,  downloadUrl {}", portFatDownload.getDownloadUrl());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT));
        Path outputPath = Path.of(portFatConfig.getBasePathZipFiele(), timestamp);
        Path outputFilesPath = Path.of(outputPath.toString(), PATH_FIELS);
        String fileName = UUID.randomUUID().toString();
        Path zipFilePath = outputPath.resolve(fileName + portFatConfig.getZipExtension());
        return createDirectories(outputPath)
                .then(createDirectories(outputFilesPath))
                .then(webClient.downloadFileAsByteArray(portFatDownload.getDownloadUrl(), zipFilePath))
                .then(Mono.fromCallable(() -> computeSHA256(zipFilePath)))
                .doOnNext(hash -> log.info("SHA-256 Hash: {}", hash))
                .flatMap(hash -> {
                    portFatDownload.setSha256(hash);
                    return portFatDownloadDAO.updatePortFatDownload(portFatDownload);
                })
                .then(unzip(zipFilePath.toString(), outputFilesPath.toString()))
                .thenMany(processDirectory(outputFilesPath))
                .then()
                .doOnTerminate(() -> deleteFileOrDirectory(outputPath.toFile()).subscribe());
    }

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

    private Mono<Void> processFile(Path file, String parentDirectoryName) {
        log.info("Processing file: {} in folder: {}", file, parentDirectoryName);
        return Mono.fromCallable(() -> convertToObject(file.toFile(), PortaleFatturazioneModel.class))
                .flatMap(portaleFatturazioneModel ->
                        Mono.zip(
                                        Mono.fromCallable(() -> computeSHA256(file)),
                                        Mono.fromCallable(() -> jsonToByteArray(portaleFatturazioneModel))
                                )
                                .flatMap(tuple -> {
                                    String sha256 = tuple.getT1();
                                    byte[] jsonToByteArray = tuple.getT2();
                                    FileCreationWithContentRequest fileCreationRequest = mapper(jsonToByteArray, portaleFatturazioneModel);
                                    return safeStorageService.createAndUploadContent(fileCreationRequest, sha256);
                                })
                )
                .onErrorResume(e -> {
                    log.error("Error processing file: {}", file, e);
                    return Mono.error(e);
                }).then();
    }

}
