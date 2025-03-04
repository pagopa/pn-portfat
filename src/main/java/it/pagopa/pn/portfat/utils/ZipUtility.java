package it.pagopa.pn.portfat.utils;

import it.pagopa.pn.portfat.exception.PnGenericException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.ZIP_ERROR;

@Slf4j
public class ZipUtility {

    private ZipUtility() {
        throw new IllegalCallerException();
    }

    public static Mono<Void> unzip(String zipFilePath, String destDirectory) {
        return Mono.fromCallable(() -> createDestinationDirectory(destDirectory, zipFilePath))
                .flatMapMany(fis -> Flux.using(
                        () -> new ZipInputStream(fis),
                        zis -> Flux.generate(sink -> processZipEntry(zis, destDirectory, sink)),
                        ZipUtility::closeZipStream
                ))
                .doOnComplete(() -> log.info("Unzip operation completed"))
                .then();
    }

    private static FileInputStream createDestinationDirectory(String destDirectory, String zipFilePath) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        return new FileInputStream(zipFilePath);
    }

    private static void processZipEntry(ZipInputStream zis, String destDirectory, SynchronousSink<Object> sink) {
        try {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                sink.complete();
                return;
            }

            String sanitizedEntryName = sanitizeEntryName(entry.getName());
            File newFile = new File(destDirectory, sanitizedEntryName);

            if (isValidPath(newFile, destDirectory)) {
                processFileEntry(entry, zis, newFile);
                sink.next(newFile);
            } else {
                sink.error(new PnGenericException(ZIP_ERROR, "Potential Zip Slip detected: " + entry.getName()));
            }
        } catch (IOException e) {
            sink.error(new PnGenericException(ZIP_ERROR, ZIP_ERROR.getMessage() + e.getMessage()));
        }
    }

    private static String sanitizeEntryName(String entryName) {
        return entryName.replace("..", "")
                .replace("/", File.separator)
                .replace("\\", File.separator);
    }

    private static boolean isValidPath(File newFile, String destDirectory) throws IOException {
        return newFile.getCanonicalPath().startsWith(new File(destDirectory).getCanonicalPath());
    }

    private static void processFileEntry(ZipEntry entry, ZipInputStream zis, File newFile) throws IOException {
        if (entry.isDirectory()) {
            newFile.mkdirs();
        } else {
            createParentDirectoriesIfNeeded(newFile);
            writeFileContent(zis, newFile);
        }
    }

    private static void createParentDirectoriesIfNeeded(File newFile) {
        File parent = newFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
    }

    private static void writeFileContent(ZipInputStream zis, File newFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(newFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    private static void closeZipStream(ZipInputStream zis) {
        try {
            zis.close();
        } catch (IOException e) {
            log.error("Error closing ZipInputStream", e);
        }
    }

}
