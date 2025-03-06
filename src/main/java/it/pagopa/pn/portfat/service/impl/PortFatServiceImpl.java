package it.pagopa.pn.portfat.service.impl;

import it.pagopa.pn.portfat.config.HttpConnectorWebClient;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.portfat.service.PortFatService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
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

    private final PortFatPropertiesConfig portFatConfig;
    private final HttpConnectorWebClient webClient;
    private final PortFatDownloadDAO portFatDownloadDAO;
    private final SafeStorageClient safeStorageClient;

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
                .then(computeSHA256(zipFilePath)
                        .doOnSuccess(hash -> log.info("SHA-256 Hash: {}", hash))
                        .flatMap(hash -> {
                            portFatDownload.setSha256(hash);
                            return portFatDownloadDAO.updatePortFatDownload(portFatDownload);
                        }))
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

    private Mono<String> computeSHA256(Path filePath) {
        return Mono.fromCallable(() -> {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream fis = Files.newInputStream(filePath);
                 DigestInputStream dis = new DigestInputStream(fis, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // Il digest viene aggiornato automaticamente dal DigestInputStream
                }
            }
            byte[] hash = digest.digest();
            return HexFormat.of().formatHex(hash);
        }).subscribeOn(Schedulers.boundedElastic());
    }

}
