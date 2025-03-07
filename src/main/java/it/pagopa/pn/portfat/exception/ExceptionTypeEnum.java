package it.pagopa.pn.portfat.exception;

import lombok.Getter;

@Getter
public enum ExceptionTypeEnum {

    JSON_MAPPER_ERROR("JSON_MAPPER_ERROR", "Failed to map the requested object: "),
    MAPPER_ERROR("MAPPER_ERROR", "Failed to map the requested object: "),
    DOWNLOAD_ZIP_ERROR("DOWNLOAD_ZIP_ERROR", "Failed to download file: "),
    PROCESS_ERROR("PROCESS_ERROR", "Failed to process the files: "),
    CREATE_PATH_ERROR("CREATE_PATH_ERROR", "Failed to create directory: "),
    LIST_FILES_ERROR("FAILED_LIST_ERROR", "Failed to list files in directory: "),
    FAILED_DELETE_FILE("FAILED_DELETE_FILE", "Failed to delete file: "),
    ZIP_ERROR("ZIP_ERROR", "Failed to unzip the file: "),
    SHA256_ERROR("SHA256_ERROR", "Failed to create SHA-256: "),
    CREATION_FILE_SS_ERROR("CREATION_FILE_SS_ERROR", "Error in file creation flow, save storage: "),
    CONVERT_TO_JSON_ERROR("SHA256_ERROR", "Failed convert to JSON: "),
    ZIP_NOT_FOND("ZIP_NOT_FOND", "JSONS not found in the ZIP file: ");

    private final String title;
    private final String message;

    ExceptionTypeEnum(String title, String message) {
        this.title = title;
        this.message = message;
    }
}
