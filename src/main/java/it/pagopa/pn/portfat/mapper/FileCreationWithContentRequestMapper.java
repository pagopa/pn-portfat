package it.pagopa.pn.portfat.mapper;

import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;

public class FileCreationWithContentRequestMapper {

    private FileCreationWithContentRequestMapper() {
        throw new IllegalCallerException();
    }

    static final String SAVED_STATUS = "SAVED";

    public static FileCreationWithContentRequest mapper(byte[] byteArray, String contentType, String documentType) {
        FileCreationWithContentRequest request = new FileCreationWithContentRequest();
        request.setContentType(contentType);
        request.setDocumentType(documentType);
        request.setStatus(SAVED_STATUS);
        request.setContent(byteArray);
        return request;
    }

}
