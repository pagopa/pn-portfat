package it.pagopa.pn.portfat.service.impl;

import it.pagopa.pn.portfat.config.HttpConnectorWebClient;
import it.pagopa.pn.portfat.config.PortfatPropertiesConfig;
import it.pagopa.pn.portfat.service.PortFatService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
    public Mono<Void> processZipFile(String downloadUrl) {
        log.info("processZipFile,  downloadUrl {}", downloadUrl);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT));
        Path outputPath = Path.of(portFatConfig.getBasePathZipFiele(), timestamp);
        Path outputFilesPath = Path.of(outputPath.toString(), PATH_FIELS);
        String fileName = UUID.randomUUID().toString();
        Path zipFilePath = outputPath.resolve(fileName + portFatConfig.getZipExtension());

        return createDirectories(outputPath)
                .then(createDirectories(outputFilesPath))
                .then(webClient.downloadFileAsByteArray(downloadUrl, zipFilePath))
                .then(unzip(zipFilePath.toString(), outputFilesPath.toString()))
                .thenMany(processFiles(outputFilesPath))
                .then()
                .doFinally(signalType -> deleteFileOrDirectory(outputPath.toFile()).subscribe());
    }

    private Mono<Void> processFiles(Path extractedDir) {
        log.info("processFiles");
        return Mono.empty();
    }

}
