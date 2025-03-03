package it.pagopa.pn.portfat.service.impl;

import it.pagopa.pn.portfat.config.PortfatPropertiesConfig;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.service.PortFatService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.PROCESS_ERROR;
import static it.pagopa.pn.portfat.utils.Utility.deleteFileOrDirectory;
import static it.pagopa.pn.portfat.utils.ZipUtility.unzip;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newOutputStream;

@Service
@CustomLog
@AllArgsConstructor
public class PortFatServiceImpl implements PortFatService {

    private static final String TIME_FORMAT = "yyyy-MM-dd_HH-mm-ss-SSS";
    private static final String PATH_FIELS = "port-fat-files";

    private final RestTemplate restTemplate;
    private final PortfatPropertiesConfig portFatConfig;

    @Override
    public void processZipFile(String httpUrlZip) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT));
        Path outputPath = Path.of(portFatConfig.getBasePathZipFiele(), timestamp);
        Path outputFilesPath = Path.of(outputPath.toString(), PATH_FIELS);
        String fileName = UUID.randomUUID().toString();
        Path zipFilePath = outputPath.resolve(fileName + portFatConfig.getZipExtension());

        try {
            //Crea la directory principale
            createDirectories(outputPath);
            log.info("directory principale creata {}", outputPath);
            //Crea la sotto directory per i file
            createDirectories(outputFilesPath);
            log.info("sotto directory creata {}", outputFilesPath);

            //Scarica il file ZIP
            downloadFile(httpUrlZip, zipFilePath);
            log.info("downloadFile ok");

            //Estrai il file ZIP
            unzip(zipFilePath.toString(), outputFilesPath.toString());
            log.info("unzip ok");

            //Processa i singoli file estratti
            processFiles(outputFilesPath);
            log.info("processFiles ok");
        } catch (Exception ex) {
            throw new PnGenericException(PROCESS_ERROR, PROCESS_ERROR.getMessage() + ex.getMessage());
        } finally {
            deleteFileOrDirectory(outputPath.toFile());
        }
    }

    private void downloadFile(String url, Path fileOutput) throws IOException {
        try (OutputStream fileOutputStream = newOutputStream(fileOutput, StandardOpenOption.CREATE)) {
            restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
                StreamUtils.copy(clientHttpResponse.getBody(), fileOutputStream);
                return null;
            });
        }
    }

    private void processFiles(Path extractedDir) {

    }

}
