package it.pagopa.pn.portfat.utils;

import it.pagopa.pn.portfat.exception.PnGenericException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.JSONS_NOT_FOND_IN_ZIP;
import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.ZIP_ERROR;

@Slf4j
public class ZipUtility {

    private ZipUtility() {
        throw new IllegalCallerException();
    }

    public static Mono<Void> unzip(String zipFilePath, String destDirectory) {
        return Mono.fromCallable(() -> {
            prepareDestinationDirectory(destDirectory);
            return processZipFile(zipFilePath, destDirectory);
        }).then();
    }

    private static void prepareDestinationDirectory(String destDirectory) {
        File destDir = new File(destDirectory);
        if (!destDir.exists() && !destDir.mkdirs()) {
            throw new PnGenericException(ZIP_ERROR, "Failed to create destination directory: " + destDirectory);
        }
    }

    private static Void processZipFile(String zipFilePath, String destDirectory) {
        try (ZipFile zipFile = new ZipFile(new File(zipFilePath))) {
            boolean foundJson = extractJsonFiles(zipFile, destDirectory);
            if (!foundJson) {
                throw new PnGenericException(JSONS_NOT_FOND_IN_ZIP, JSONS_NOT_FOND_IN_ZIP.getMessage());
            }
        } catch (IOException e) {
            throw new PnGenericException(ZIP_ERROR, "Error processing ZIP file: " + e.getMessage());
        }
        return null;
    }

    private static boolean extractJsonFiles(ZipFile zipFile, String destDirectory) throws IOException {
        boolean foundJson = false;
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            // Controllo del path traversal (zip slip) per evitare l'estrazione in posizioni indesiderate
            File destFile = new File(destDirectory, sanitizeEntryName(zipEntry.getName()));
            // Verifica se il file estratto si trova effettivamente all'interno della directory di destinazione
            if (!destFile.toPath().normalize().startsWith(destDirectory)) {
                throw new IOException("Path traversal attempt: " + destDirectory);
            }
            if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".json")) {
                foundJson = true;
                saveZipEntry(zipFile, zipEntry, destDirectory);
            }
        }
        return foundJson;
    }

    private static void saveZipEntry(ZipFile zipFile, ZipEntry zipEntry, String destDirectory) throws IOException {
        File destFile = new File(destDirectory, sanitizeEntryName(zipEntry.getName()));

        if (!isValidPath(destFile, new File(destDirectory))) {
            throw new PnGenericException(ZIP_ERROR, "Potential Zip Slip detected: " + zipEntry.getName());
        }

        writeFileContent(zipFile, zipEntry, destFile);
        log.info("Extracted JSON: {}", destFile.getAbsolutePath());
    }

    private static String sanitizeEntryName(String entryName) {
        return entryName.replace("..", "")
                .replace("/", File.separator)
                .replace("\\", File.separator);
    }

    private static boolean isValidPath(File newFile, File destDir) throws IOException {
        return newFile.getCanonicalPath().startsWith(destDir.getCanonicalPath());
    }

    private static void writeFileContent(ZipFile zipFile, ZipEntry zipEntry, File destFile) throws IOException {
        File file = new File(destFile, zipEntry.getName());
        if (!file.toPath().normalize().startsWith(destFile.toPath())) {
            throw new PnGenericException(ZIP_ERROR, "Bad zip entry: " + zipEntry.getName());
        }
        try (InputStream is = zipFile.getInputStream(zipEntry);
             FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }
}
