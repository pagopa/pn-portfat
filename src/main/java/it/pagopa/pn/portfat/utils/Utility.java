package it.pagopa.pn.portfat.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.model.FileReadyModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import reactor.core.publisher.Mono;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.*;

/**
 * Classe di utilità per fornire metodi di supporto comuni, come la conversione tra oggetti e byte array,
 * il calcolo dell'hash SHA-256 e la gestione della serializzazione e deserializzazione JSON.
 */
@Slf4j
public class Utility {

    /**
     * Costruttore privato per evitare l'istanza della classe. Lancia un'eccezione se chiamato.
     */
    private Utility() {
        throw new IllegalCallerException();
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converte un oggetto in un array di byte JSON.
     *
     * @param jsonObject l'oggetto da convertire in JSON
     * @return un array di byte che rappresenta l'oggetto in formato JSON
     * @throws PnGenericException se si verifica un errore durante la conversione
     */
    public static byte[] jsonToByteArray(Object jsonObject) {
        try {
            return objectMapper.writeValueAsBytes(jsonObject);
        } catch (Exception e) {
            throw new PnGenericException(CONVERT_TO_JSON_ERROR, CONVERT_TO_JSON_ERROR.getMessage() + e.getMessage());
        }
    }

    /**
     * Calcola l'hash SHA-256 di un file dato un percorso.
     *
     * @param filePath il percorso del file di cui calcolare l'hash SHA-256
     * @return una stringa che rappresenta l'hash SHA-256 del file in formato esadecimale
     * @throws PnGenericException se si verifica un errore durante il calcolo dell'hash
     */
    public static String computeSHA256(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(Files.newInputStream(filePath), digest)) {
                while (dis.read() != -1) {
                    // Il DigestInputStream aggiorna automaticamente il digest
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new PnGenericException(SHA256_ERROR, SHA256_ERROR.getMessage() + e.getMessage());
        }
    }

    /**
     * Converte una stringa JSON in un oggetto di tipo specificato.
     *
     * @param body la stringa JSON da convertire
     * @param tClass la classe di tipo in cui mappare la stringa JSON
     * @param <T> il tipo dell'oggetto di destinazione
     * @return l'oggetto mappato dalla stringa JSON
     * @throws PnGenericException se si verifica un errore durante la mappatura
     */
    public static <T> T convertToObject(String body, Class<T> tClass) {
        T entity = jsonToObject(body, tClass);
        if (entity == null) throw new PnGenericException(MAPPER_ERROR, MAPPER_ERROR.getMessage());
        return entity;
    }

    /**
     * Calcola un ID univoco per il download basato sull'URL e sulla versione del file.
     *
     * @param fileReady l'oggetto che contiene l'URL e la versione del file
     * @return una stringa che rappresenta l'ID univoco del download
     */
    public static String downloadId(FileReadyModel fileReady) {
        return fileReady.getDownloadUrl() + fileReady.getFileVersion();
    }

    /**
     * Converte un file in un oggetto di tipo specificato.
     *
     * @param file il file da convertire
     * @param tClass la classe di tipo in cui mappare il contenuto del file
     * @param <T> il tipo dell'oggetto di destinazione
     * @return l'oggetto mappato dal file
     * @throws PnGenericException se si verifica un errore durante la lettura del file
     */
    public static <T> T convertToObject(File file, Class<T> tClass) {
        try {
            return objectMapper.readValue(file, tClass);
        } catch (IOException e) {
            log.error("Error reading JSON file: {}", e.getMessage(), e);
            throw new PnGenericException(JSON_MAPPER_ERROR, JSON_MAPPER_ERROR.getMessage());
        }
    }

    /**
     * Converte una stringa JSON in un oggetto di tipo specificato. Questo è un metodo privato utilizzato internamente.
     *
     * @param json la stringa JSON da convertire
     * @param tClass la classe di tipo in cui mappare la stringa JSON
     * @param <T> il tipo dell'oggetto di destinazione
     * @return l'oggetto mappato dalla stringa JSON
     */
    private static <T> T jsonToObject(String json, Class<T> tClass) {
        try {
            return objectMapper.readValue(json, tClass);
        } catch (JsonProcessingException e) {
            log.error("Error with mapping : {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Delete File or Directory in a thread-safe manner.
     *
     * @param workPath path or file to delete
     */
    public static Mono<Void> deleteFileOrDirectory(File workPath) {
        return Mono.fromCallable(() -> {
            synchronized(Utility.class) {
                if (workPath.exists()) {
                    FileUtils.forceDelete(workPath);
                    log.info("Directory deleted: {}", workPath);
                } else {
                    log.info("Directory already deleted: {}", workPath);
                }
            }
            return workPath;
        }).then();
    }

    /**
     * Create Directory.
     *
     * @param path path or file to delete
     */
    public static Mono<Void> createDirectories(Path path) {
        return Mono.fromCallable(() -> {
            Files.createDirectories(path);
            log.info("Directory created: {}", path);
            return path;
        }).then();
    }
}
