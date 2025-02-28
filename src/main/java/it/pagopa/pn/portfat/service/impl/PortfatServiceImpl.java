package it.pagopa.pn.portfat.service.impl;

import it.pagopa.pn.portfat.service.PortfatService;
import it.pagopa.pn.portfat.utils.UnzipUtility;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
@CustomLog
@AllArgsConstructor
public class PortfatServiceImpl implements PortfatService {

    private final RestTemplate restTemplate;

    @Override
    public void processZipFile(String url, HttpMethod method, String outputDir, String fileName) {
        Path outputPath = Path.of(outputDir);
        Path zipFilePath = outputPath.resolve(fileName);

        try {
            // 1️ Crea la directory
            Files.createDirectories(outputPath);
            // 2️ Scarica il file ZIP
            downloadFile(url, method, zipFilePath);
            // 3️ Estrai il file ZIP
            UnzipUtility.unzip(zipFilePath.toString(), outputPath.toString());
            // 4️ Processa i singoli file estratti
            processExtractedFiles(outputPath);
        } catch (Exception ex) {
            throw new RuntimeException("Errore nel processo del file: " + ex.getMessage(), ex);
        } finally {
            // 5️ Elimina la cartella dopo la lavorazione
            deleteDirectory(outputPath);
        }
    }

    private void downloadFile(String url, HttpMethod method, Path fileOutput) throws IOException {
        try (OutputStream fileOutputStream = Files.newOutputStream(fileOutput, StandardOpenOption.CREATE)) {
            restTemplate.execute(url, method, null, clientHttpResponse -> {
                StreamUtils.copy(clientHttpResponse.getBody(), fileOutputStream);
                return null;
            });
        }
    }

    private void processExtractedFiles(Path extractedDir) {

    }

    private void deleteDirectory(Path directory) {

    }
}
