package it.pagopa.pn.portfat.middleware.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.portfat.config.PortfatPropertiesConfig;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import it.pagopa.pn.portfat.utils.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.MAPPER_ERROR;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueListener {

    private final ObjectMapper objectMapper;
    private final PortfatPropertiesConfig portfatConfig;

    @SqsListener(value = "${pn.pn-portfat.queue}", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void pullPortFat(@Payload String payload) {
        log.info("pullPortFat {}", payload);

        FileReadyEvent fileReadyEvent = convertToObject(payload, FileReadyEvent.class);
        boolean isUriValid = isFileReadyEvent(fileReadyEvent);

        if (isUriValid) {

        }
    }

    private boolean isFileReadyEvent(FileReadyEvent fileReadyEvent) {
        String downloadUrl = fileReadyEvent.getDownloadUrl();
        return downloadUrl.startsWith(portfatConfig.getBlobStorageBaseUrl())
                && portfatConfig.getFilePathWhiteList().stream().anyMatch(downloadUrl::contains)
                && fileReadyEvent.getFileVersionString() != null
                && !fileReadyEvent.getFileVersionString().trim().isBlank();
    }

    private <T> T convertToObject(String body, Class<T> tClass) {
        T entity = Utility.jsonToObject(this.objectMapper, body, tClass);
        if (entity == null) throw new PnGenericException(MAPPER_ERROR, MAPPER_ERROR.getMessage());
        return entity;
    }

}
