package it.pagopa.pn.portfat.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;

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
    public static void deleteFileOrDirectory(File workPath) {
        try {
            FileUtils.forceDelete(workPath);
        } catch (Exception e) {
            log.info("The following path could not be deleted {} , ERROR: {}", workPath.getPath(), e.getMessage());
        }
    }
}
