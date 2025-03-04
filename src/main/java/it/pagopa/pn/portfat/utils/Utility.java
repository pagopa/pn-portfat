package it.pagopa.pn.portfat.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class Utility {

    private Utility() {
        throw new IllegalCallerException();
    }

    public static <T> T jsonToObject(ObjectMapper objectMapper, String json, Class<T> tClass) {
        try {
            return objectMapper.readValue(json, tClass);
        } catch (JsonProcessingException e) {
            log.error("Error with mapping : {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Delete File or Directory.
     *
     * @param workPath path or file to delete
     */
    public static Mono<Void> deleteFileOrDirectory(File workPath) {
        return Mono.fromCallable(() -> {
            log.info("Directory deleted: {}", workPath);
            FileUtils.forceDelete(workPath);
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
            log.info("Directory created: {}", path);
            Files.createDirectories(path);
            return path;
        }).then();
    }
}
