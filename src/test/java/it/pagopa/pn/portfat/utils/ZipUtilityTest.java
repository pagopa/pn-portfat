package it.pagopa.pn.portfat.utils;

import it.pagopa.pn.portfat.exception.PnGenericException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ZipUtilityTest {

    private static Path tempDir;

    @BeforeAll
    static void setup() throws IOException {
        Path resourceDir = Paths.get("src", "test", "resources", "zip-test");
        tempDir = Files.createDirectory(resourceDir);
    }

    @AfterAll
    static void cleanup() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void testUnzip_SuccessfullyExtractsJson() throws IOException {
        // Arrange: Crea un file ZIP con un JSON all'interno
        Path zipFile = tempDir.resolve("test.zip");
        createZipWithJson(zipFile, "test.json", "{\"key\":\"value\"}");

        // Act & Assert
        StepVerifier.create(ZipUtility.unzip(zipFile.toString(), tempDir.toString()))
                .verifyComplete();

        // Verifica che il file JSON sia stato estratto
        File extractedJson = tempDir.resolve("test.json").toFile();
        assertTrue(extractedJson.exists(), "Il file JSON non è stato estratto");
    }

    @Test
    void testUnzip_ThrowsExceptionWhenNoJsonFound() throws IOException {
        // Arrange: Crea un file ZIP senza JSON
        Path zipFile = tempDir.resolve("empty.zip");
        createZipWithJson(zipFile, "file.txt", "Test non JSON");

        // Act & Assert
        StepVerifier.create(ZipUtility.unzip(zipFile.toString(), tempDir.toString()))
                .expectErrorMatches(throwable ->
                        throwable instanceof PnGenericException &&
                                throwable.getMessage().contains("No JSON files found"))
                .verify();
    }


    @Test
    void testUnzip_ThrowsExceptionOnInvalidZip() {
        // Arrange: Crea un file ZIP corrotto
        Path invalidZipFile = tempDir.resolve("invalid.zip");
        try (FileOutputStream fos = new FileOutputStream(invalidZipFile.toFile())) {
            fos.write(new byte[]{0, 1, 2, 3, 4}); // Dati non validi
        } catch (IOException e) {
            fail("Errore nella creazione del file ZIP corrotto");
        }

        // Act & Assert
        StepVerifier.create(ZipUtility.unzip(invalidZipFile.toString(), tempDir.toString()))
                .expectErrorMatches(throwable ->
                        throwable instanceof PnGenericException &&
                                throwable.getMessage().contains("No JSON files found in the ZIP archive"))
                .verify();
    }


    // Metodo per creare un ZIP contenente un file JSON
    private void createZipWithJson(Path zipFile, String fileName, String fileContent) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            zos.write(fileContent.getBytes());
            zos.closeEntry();
        }
    }

    @Test
    void testUnzip_ThrowsExceptionOnZipSlipAttempt() throws IOException {
        // Arrange: Crea un file ZIP con un Zip Slip Attack
        Path zipFile = tempDir.resolve("portFatt.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            ZipEntry zipEntry = new ZipEntry("nested/../../malicious.json");
            zos.putNextEntry(zipEntry);
            zos.write("{\"attack\":\"true\"}".getBytes());
            zos.closeEntry();
        }

        // Act & Assert
        StepVerifier.create(ZipUtility.unzip(zipFile.toString(), tempDir.toString()))
                .expectErrorMatches(throwable ->
                        throwable instanceof PnGenericException &&
                                throwable.getMessage().contains("Error processing ZIP file"))
                .verify();
    }
}