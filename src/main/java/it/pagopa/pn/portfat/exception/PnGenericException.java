package it.pagopa.pn.portfat.exception;

import lombok.Getter;

@Getter
public class PnGenericException extends RuntimeException {

    private final ExceptionTypeEnum exceptionType;
    private final String message;

    public PnGenericException(ExceptionTypeEnum exceptionType, String message) {
        super(message);
        this.exceptionType = exceptionType;
        this.message = message;
    }

}
