package it.pagopa.pn.portfat.utils;

import it.pagopa.pn.portfat.exception.PnGenericException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.ZIP_ERROR;
import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.ZIP_NOT_FOND;

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
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zipInputStream = new ZipInputStream(fis)) {

            boolean foundJson = extractJsonFiles(zipInputStream, destDirectory);
            if (!foundJson) {
                throw new PnGenericException(ZIP_NOT_FOND, "No JSON files found in the ZIP archive.");
            }
        } catch (IOException e) {
            throw new PnGenericException(ZIP_ERROR, "Error processing ZIP file: " + e.getMessage());
        }
        return null;
    }

    private static boolean extractJsonFiles(ZipInputStream zipInputStream, String destDirectory) throws IOException {
        boolean foundJson = false;
        ZipEntry zipEntry;

        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            if (zipEntry.getName().endsWith(".json")) {
                foundJson = true;
                saveZipEntry(zipEntry, zipInputStream, destDirectory);
            }
        }
        return foundJson;
    }

    private static void saveZipEntry(ZipEntry zipEntry, ZipInputStream zipInputStream, String destDirectory) throws IOException {
        File newFile = new File(destDirectory, sanitizeEntryName(zipEntry.getName()));

        if (!isValidPath(newFile, new File(destDirectory))) {
            throw new PnGenericException(ZIP_ERROR, "Potential Zip Slip detected: " + zipEntry.getName());
        }

        writeFileContent(zipInputStream, newFile);
        log.info("Extracted JSON: {}", newFile.getAbsolutePath());
    }

    private static String sanitizeEntryName(String entryName) {
        return entryName.replace("..", "")
                .replace("/", File.separator)
                .replace("\\", File.separator);
    }

    private static boolean isValidPath(File newFile, File destDir) throws IOException {
        return newFile.getCanonicalPath().startsWith(destDir.getCanonicalPath());
    }

    private static void writeFileContent(ZipInputStream zis, File newFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(newFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = zis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }
}
