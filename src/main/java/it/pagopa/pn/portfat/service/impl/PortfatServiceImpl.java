package it.pagopa.pn.portfat.service.impl;

import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.service.PortfatService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.DOWNLOAD_ZIP_ERROR;

@Service
@CustomLog
@AllArgsConstructor
public class PortfatServiceImpl implements PortfatService {

    private final RestTemplate restTemplate;

    /**
     * generalized method for http client to download the file.
     *
     * @param url    of the external service
     * @param method type of the method to invoke(GET POST...)
     */
    public void getFile(String url, HttpMethod method, String outputDir, String fileName) {
        try {
            Path outputPath = Path.of(outputDir, fileName);
            Files.createDirectories(outputPath.getParent());
            try (OutputStream fileOutputStream = Files.newOutputStream(outputPath, StandardOpenOption.CREATE)) {
                restTemplate.execute(url, method, null, clientHttpResponse -> {
                    StreamUtils.copy(clientHttpResponse.getBody(), fileOutputStream);
                    return null;
                });
            }
        } catch (Exception ex) {
            throw new PnGenericException(DOWNLOAD_ZIP_ERROR, DOWNLOAD_ZIP_ERROR.getMessage() + ex.getMessage());
        }
    }

    public void processZipFile(String url, HttpMethod method, String outputDir, String fileName) {
        Path outputPath = Path.of(outputDir);
        Path zipFilePath = outputPath.resolve(fileName);

        try {
            // 1️⃣ Crea la directory
            Files.createDirectories(outputPath);

            // 2️⃣ Scarica il file ZIP
            downloadFile(url, method, zipFilePath);

            // 3️⃣ Estrai il file ZIP
            unzipFile(zipFilePath, outputPath);

            // 4️⃣ Processa i singoli file estratti
            processExtractedFiles(outputPath);

        } catch (Exception ex) {
            throw new RuntimeException("❌ Errore nel processo del file: " + ex.getMessage(), ex);
        } finally {
            // 5️⃣ Elimina la cartella dopo la lavorazione
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
        System.out.println("✅ File ZIP scaricato in: " + fileOutput.toAbsolutePath());
    }

    private void unzipFile(Path zipFilePath, Path destinationDir) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path filePath = destinationDir.resolve(entry.getName());
                if (!entry.isDirectory()) {
                    Files.createDirectories(filePath.getParent()); // Crea cartelle necessarie
                    try (OutputStream fileOutputStream = Files.newOutputStream(filePath)) {
                        zipInputStream.transferTo(fileOutputStream);
                    }
                }
                zipInputStream.closeEntry();
            }
        }
        System.out.println("✅ File ZIP estratto in: " + destinationDir.toAbsolutePath());
    }

    private void processExtractedFiles(Path extractedDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extractedDir)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    System.out.println("📄 Processando file: " + file.getFileName());
                    // Implementa la tua logica di elaborazione qui
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("❌ Errore durante l'elaborazione dei file: " + e.getMessage(), e);
        }
    }

    private void deleteDirectory(Path directory) {
        try {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
        }
    }
}
