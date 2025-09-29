package it.pagopa.pn.portfat.mapper;

import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import it.pagopa.pn.portfat.model.PortaleFatturazioneModel;
import org.springframework.http.MediaType;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileCreationWithContentRequestMapper {

    private FileCreationWithContentRequestMapper() {
        throw new IllegalCallerException();
    }

    static final String PN_SERVICE_ORDER = "PN_SERVICE_ORDER";
    static final String SAVED_STATUS = "SAVED";
    static final String SENDER_PA_ID = "sender_pa_id";
    static final String REFERENCE_PERIOD = "reference_period_year_month";
    static final String ORIGINAL_DATA_UPDATE = "original_data_update_timestamp";
    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm.ssX");

    public static FileCreationWithContentRequest mapper(byte[] bytesPdf, PortaleFatturazioneModel model) {
        FileCreationWithContentRequest request = new FileCreationWithContentRequest();
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setDocumentType(PN_SERVICE_ORDER);
        request.setStatus(SAVED_STATUS);
        request.setContent(bytesPdf);
        Map<String, List<String>> tags = new HashMap<>();
        tags.put(SENDER_PA_ID, List.of(model.getFkIdEnte()));
        tags.put(REFERENCE_PERIOD, List.of(model.getPeriodoRiferimento()));

        // TODO ORIGINAL_DATA_UPDATE
        tags.put(ORIGINAL_DATA_UPDATE, List.of(ZonedDateTime.now().format(formatter)));
        request.setTags(tags);
        return request;
    }

}
