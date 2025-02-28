package it.pagopa.pn.portfat.middleware.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import it.pagopa.pn.portfat.utils.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.MAPPER_ERROR;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueListener {

    private final ObjectMapper objectMapper;

    @SqsListener(value = "${pn.pn-portfat.queue}", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void pullPortFat(@Payload String node) {
        log.info("pullPortFat {}", node);
        FileReadyEvent fileReadyEvent = convertToObject(node, FileReadyEvent.class);
    }

    private <T> T convertToObject(String body, Class<T> tClass) {
        T entity = Utility.jsonToObject(this.objectMapper, body, tClass);
        if (entity == null) throw new PnGenericException(MAPPER_ERROR, MAPPER_ERROR.getMessage());
        return entity;
    }

}
