package it.pagopa.pn.portfat.exception;

import lombok.Getter;

@Getter
public enum ExceptionTypeEnum {

    MAPPER_ERROR("MAPPER_ERROR", "Non è stato possibile mappare l'oggetto richiesto"),
    DOWNLOAD_ZIP_ERROR("DOWNLOAD_ZIP_ERROR", "Non è stato possibile scaricare il file: "),
    PROCESS_ERROR("PROCESS_ERROR", "Non è stato possibile processare i file: "),
    ZIP_ERROR("ZIP_ERROR", "Non è stato possibile decomprimere il file: ");

    private final String title;
    private final String message;

    ExceptionTypeEnum(String title, String message) {
        this.title = title;
        this.message = message;
    }
}
