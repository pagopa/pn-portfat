package it.pagopa.pn.portfat.service.impl;

import it.pagopa.pn.portfat.config.HttpConnectorWebClient;
import it.pagopa.pn.portfat.config.PortfatPropertiesConfig;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.service.PortFatService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.portfat.utils.Utility.createDirectories;
import static it.pagopa.pn.portfat.utils.Utility.deleteFileOrDirectory;
import static it.pagopa.pn.portfat.utils.ZipUtility.unzip;

@Service
@CustomLog
@AllArgsConstructor
public class PortFatServiceImpl implements PortFatService {

    private static final String TIME_FORMAT = "yyyy-MM-dd_HH-mm-ss-SSS";
    private static final String PATH_FIELS = "port-fat-files";

    private final PortfatPropertiesConfig portFatConfig;
    private final HttpConnectorWebClient webClient;

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
                .then(unzip(zipFilePath.toString(), outputFilesPath.toString()))
                .thenMany(processDirectory(outputFilesPath))
                .then()
                .doFinally(signalType -> deleteFileOrDirectory(outputPath.toFile()).subscribe());
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
                })
                .then();
    }

    private Mono<Void> processFile(Path file, String parentDirectoryName) {
        return Mono.fromRunnable(() -> {
            try {
                log.info("Processing file: {} in folder: {}", file, parentDirectoryName);

                //TODO safe storage
                Files.delete(file);
                log.info("File deleted: {}", file);
            } catch (IOException e) {
                throw new PnGenericException(FAILED_DELETE_FILE, FAILED_DELETE_FILE.getMessage() + e.getMessage());
            }
        });
    }

}
