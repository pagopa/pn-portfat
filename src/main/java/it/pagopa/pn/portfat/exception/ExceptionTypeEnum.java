package it.pagopa.pn.portfat.exception;

import lombok.Getter;

@Getter
public enum ExceptionTypeEnum {

    MAPPER_ERROR("MAPPER_ERROR", "Failed to map the requested object"),
    DOWNLOAD_ZIP_ERROR("DOWNLOAD_ZIP_ERROR", "Failed to download file: "),
    PROCESS_ERROR("PROCESS_ERROR", "Failed to process the files: "),
    CREATE_PATH_ERROR("CREATE_PATH_ERROR", "Failed to create directory: "),
    LIST_FILES_ERROR("FAILED_LIST_ERROR", "Failed to list files in directory: "),
    FAILED_DELETE_FILE("FAILED_DELETE_FILE", "Failed to delete file: "),
    ZIP_ERROR("ZIP_ERROR", "Failed to unzip the file: ");

    private final String title;
    private final String message;

    ExceptionTypeEnum(String title, String message) {
        this.title = title;
        this.message = message;
    }
}
