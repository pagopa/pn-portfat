package it.pagopa.pn.portfat.mapper;

import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import it.pagopa.pn.portfat.model.PortaleFatturazioneModel;
import org.springframework.http.MediaType;

public class FileCreationWithContentRequestMapper {

    private FileCreationWithContentRequestMapper() {
        throw new IllegalCallerException();
    }

    static final String PN_SERVICE_ORDER = "PN_SERVICE_ORDER";
    static final String SAVED_STATUS = "SAVED";

    public static FileCreationWithContentRequest mapper(byte[] bytesPdf, PortaleFatturazioneModel model) {
        FileCreationWithContentRequest request = new FileCreationWithContentRequest();
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setDocumentType(PN_SERVICE_ORDER);
        request.setStatus(SAVED_STATUS);
        request.setContent(bytesPdf);
        return request;
    }

}
