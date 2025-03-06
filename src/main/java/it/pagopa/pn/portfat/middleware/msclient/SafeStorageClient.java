package it.pagopa.pn.portfat.middleware.msclient;

import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

public interface SafeStorageClient {

    String SEND_SERVICE_ORDER = "SEND_SERVICE_ORDER";
    String SAVED_STATUS = "SAVED";

    default FileCreationRequestDto buildFileCreationWithContentRequest() {
        FileCreationRequestDto request = new FileCreationRequestDto();
        request.setContentType(MediaType.APPLICATION_PDF_VALUE);
        request.setDocumentType(SEND_SERVICE_ORDER);
        request.setStatus(SAVED_STATUS);
        return request;
    }

    Mono<FileCreationResponseDto> createFile(FileCreationRequestDto fileCreationRequestDto, String safeStorageCxId,  String sha256);
}
