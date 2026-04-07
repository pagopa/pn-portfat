package it.pagopa.pn.portfat.exception;

public class PnPortfatDownloadNotFoundException extends PnGenericException{
    public PnPortfatDownloadNotFoundException(ExceptionTypeEnum exceptionType, String message) {
        super(exceptionType, message);
    }
}
