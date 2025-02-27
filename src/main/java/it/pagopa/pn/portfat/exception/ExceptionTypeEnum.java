package it.pagopa.pn.portfat.exception;

import lombok.Getter;

@Getter
public enum ExceptionTypeEnum {

    MAPPER_ERROR("MAPPER_ERROR", "Non è stato possibile mappare l'oggetto richiesto");

    private final String title;
    private final String message;

    ExceptionTypeEnum(String title, String message) {
        this.title = title;
        this.message = message;
    }
}
