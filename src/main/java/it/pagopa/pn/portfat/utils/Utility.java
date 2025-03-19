package it.pagopa.pn.portfat.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
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


@Slf4j
public class Utility {

    private Utility() {
        throw new IllegalCallerException();
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static byte[] jsonToByteArray(Object jsonObject) {
        try {
            return objectMapper.writeValueAsBytes(jsonObject);
        } catch (Exception e) {
            throw new PnGenericException(CONVERT_TO_JSON_ERROR, CONVERT_TO_JSON_ERROR.getMessage() + e.getMessage());
        }
    }

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

    public static <T> T convertToObject(String body, Class<T> tClass) {
        T entity = jsonToObject(body, tClass);
        if (entity == null) throw new PnGenericException(MAPPER_ERROR, MAPPER_ERROR.getMessage());
        return entity;
    }

    public static String downloadId(FileReadyEvent fileReady) {
        return fileReady.getDownloadUrl() + fileReady.getFileVersion();
    }

    public static <T> T convertToObject(File file, Class<T> tClass) {
        try {
            return objectMapper.readValue(file, tClass);
        } catch (IOException e) {
            log.error("Error reading JSON file: {}", e.getMessage(), e);
            throw new PnGenericException(JSON_MAPPER_ERROR, JSON_MAPPER_ERROR.getMessage());
        }
    }

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
