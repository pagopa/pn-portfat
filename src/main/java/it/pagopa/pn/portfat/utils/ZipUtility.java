package it.pagopa.pn.portfat.utils;

import it.pagopa.pn.portfat.exception.PnGenericException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.ZIP_ERROR;

public class ZipUtility {

    private ZipUtility() {
        throw new IllegalCallerException();
    }

    public static void unzip(String zipFilePath, String destDirectory) {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDirectory, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new PnGenericException(ZIP_ERROR, ZIP_ERROR.getMessage() + e.getMessage());
        }
    }
}
