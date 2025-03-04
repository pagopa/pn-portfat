package it.pagopa.pn.portfat.utils;

import it.pagopa.pn.portfat.exception.PnGenericException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
        return Mono.fromCallable(() -> {
                    File destDir = new File(destDirectory);
                    if (!destDir.exists() && !destDir.mkdirs()) {
                        throw new IOException("Failed to create directory: " + destDirectory);
                    }
                    return new FileInputStream(zipFilePath);
                })
                .flatMapMany(fis -> Flux.using(
                        () -> new ZipInputStream(fis),
                        zis -> Flux.generate(sink -> {
                            try {
                                ZipEntry entry = zis.getNextEntry();
                                if (entry == null) {
                                    sink.complete();
                                    return;
                                }

                                File newFile = new File(destDirectory, entry.getName());
                                if (entry.isDirectory()) {
                                    if (!newFile.mkdirs() && !newFile.exists()) {
                                        throw new IOException("Failed to create directory: " + newFile);
                                    }
                                } else {
                                    File parent = newFile.getParentFile();
                                    if (!parent.exists() && !parent.mkdirs()) {
                                        throw new IOException("Failed to create parent directory: " + parent);
                                    }

                                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                                        byte[] buffer = new byte[4096];
                                        int len;
                                        while ((len = zis.read(buffer)) > 0) {
                                            fos.write(buffer, 0, len);
                                        }
                                    }
                                }
                                sink.next(newFile);
                            } catch (IOException e) {
                                sink.error(new PnGenericException(ZIP_ERROR, ZIP_ERROR.getMessage() + e.getMessage()));
                            }
                        }),
                        zis -> {
                            try {
                                zis.close();
                            } catch (IOException e) {
                                log.error("Error closing ZipInputStream", e);
                            }
                        }
                ))
                .doOnComplete(() -> log.info("Unzip operation completed"))
                .then();
    }

}
