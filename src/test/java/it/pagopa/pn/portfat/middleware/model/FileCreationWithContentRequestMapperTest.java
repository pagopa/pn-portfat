package it.pagopa.pn.portfat.middleware.model;

import it.pagopa.pn.portfat.mapper.FileCreationWithContentRequestMapper;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileCreationWithContentRequestMapperTest {

    @Test
    void testMapperSuccess() {
        byte[] content = new byte[]{1, 2, 3};
        String contentType = "application/json";
        String documentType = "DOC_TYPE";

        FileCreationWithContentRequest result =
                FileCreationWithContentRequestMapper.mapper(content, contentType, documentType);

        assertNotNull(result);
        assertArrayEquals(content, result.getContent());
        assertEquals(contentType, result.getContentType());
        assertEquals(documentType, result.getDocumentType());
        assertEquals("SAVED", result.getStatus());
    }

}