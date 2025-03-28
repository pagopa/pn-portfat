package it.pagopa.pn.portfat.utils;

import it.pagopa.pn.portfat.exception.PnGenericException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.JSONS_NOT_FOND_IN_ZIP;
import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.ZIP_ERROR;

/**
 * Classe di utilità per la gestione e l'estrazione di file ZIP.
 * Fornisce metodi per decomprimere file ZIP e estrarre file JSON in una destinazione specificata.
 */
@Slf4j
public class ZipUtility {

    /**
     * Costruttore privato per impedire l'istanza della classe. Lancia un'eccezione se chiamato.
     */
    private ZipUtility() {
        throw new IllegalCallerException();
    }

    /**
     * Estrae un file ZIP in una directory di destinazione specificata.
     *
     * @param zipFilePath   il percorso del file ZIP da estrarre
     * @param destDirectory la directory di destinazione per i file estratti
     * @return un Mono vuoto che rappresenta l'operazione asincrona di estrazione
     * @throws PnGenericException se si verifica un errore durante l'estrazione
     */
    public static Mono<Void> unzip(String zipFilePath, String destDirectory) {
        return Mono.fromCallable(() -> {
            prepareDestinationDirectory(destDirectory);
            return processZipFile(zipFilePath, destDirectory);
        }).then();
    }

    /**
     * Verifica la validità della directory di destinazione.
     *
     * @param destDirectory la directory di destinazione da preparare
     * @throws PnGenericException se non è possibile creare la directory di destinazione
     */
    private static void prepareDestinationDirectory(String destDirectory) {
        File destDir = new File(destDirectory);
        if (!destDir.exists() && !destDir.mkdirs()) {
            throw new PnGenericException(ZIP_ERROR, "Failed to create destination directory: " + destDirectory);
        }
    }

    /**
     * Processa il file ZIP, estraendo i file JSON contenuti al suo interno nella directory di destinazione.
     *
     * @param zipFilePath   il percorso del file ZIP da estrarre
     * @param destDirectory la directory di destinazione per i file estratti
     * @return un valore void dopo aver completato l'estrazione
     * @throws PnGenericException se si verifica un errore durante il processo del file ZIP
     */
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

    /**
     * Estrae i file JSON dal file ZIP e li salva nella directory di destinazione.
     *
     * @param zipFile       il file ZIP da cui estrarre i file
     * @param destDirectory la directory di destinazione per i file estratti
     * @return true se sono stati trovati e estratti file JSON, altrimenti false
     * @throws IOException se si verifica un errore durante l'estrazione dei file
     */
    private static boolean extractJsonFiles(ZipFile zipFile, String destDirectory) throws IOException {
        boolean foundJson = false;
        Path destDirPath = Paths.get(destDirectory).toAbsolutePath().normalize();
        List<? extends ZipEntry> zipEntries = zipFile.stream().toList();

        for (ZipEntry zipEntry : zipEntries) {
            Path entryPath = destDirPath.resolve(zipEntry.getName()).normalize();
            if (!entryPath.startsWith(destDirPath)) {
                throw new IOException("Path traversal attempt detected: " + zipEntry.getName());
            }
            if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".json")) {
                foundJson = true;
                saveZipEntry(zipFile, zipEntry, destDirectory);
            }
        }
        return foundJson;
    }

    /**
     * Salva il contenuto nella directory di destinazione.
     *
     * @param zipFile       il file ZIP da cui estrarre la voce
     * @param zipEntry      la voce ZIP da estrarre
     * @param destDirectory la directory di destinazione per il file estratto
     * @throws IOException se si verifica un errore durante il salvataggio del file estratto
     */
    private static void saveZipEntry(ZipFile zipFile, ZipEntry zipEntry, String destDirectory) throws IOException {
        File destFile = new File(destDirectory, sanitizeEntryName(zipEntry.getName()));
        writeFileContent(zipFile, zipEntry, destFile);
        log.info("Extracted JSON: {}", destFile.getName());
    }

    /**
     * Sanifica il nome di una voce ZIP per evitare percorsi non sicuri.
     *
     * @param entryName il nome della voce ZIP da sanificare
     * @return il nome sanificato della voce ZIP
     */
    private static String sanitizeEntryName(String entryName) {
        return entryName.replace("..", "")
                .replace("/", File.separator)
                .replace("\\", File.separator);
    }

    /**
     * Scrive il contenuto di una voce ZIP estratta in un file di destinazione.
     *
     * @param zipFile  il file ZIP da cui estrarre il contenuto
     * @param zipEntry la voce ZIP contenente il contenuto da scrivere
     * @param destFile il file di destinazione in cui scrivere il contenuto
     * @throws IOException se si verifica un errore durante la scrittura del file
     */
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
