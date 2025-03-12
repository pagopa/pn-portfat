package it.pagopa.pn.portfat.model;

import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationRequestDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileCreationWithContentRequest extends FileCreationRequestDto {
    private byte[] content;
}
