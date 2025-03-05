package it.pagopa.pn.portfat.middleware.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.portfat.config.PortfatPropertiesConfig;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import static it.pagopa.pn.portfat.middleware.db.converter.PortFatConverter.portFatDownloadToEntity;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.DownloadStatus;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.service.PortFatService;
import it.pagopa.pn.portfat.utils.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.MAPPER_ERROR;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueListener {

    private final ObjectMapper objectMapper;
    private final PortfatPropertiesConfig portFatConfig;
    private final PortFatService portfatService;
    private final PortFatDownloadDAO portFatDownloadDAO;

    @SqsListener(value = "${pn.pn-portfat.queue}", deletionPolicy = SqsMessageDeletionPolicy.DEFAULT)
    public void pullPortFat(@Payload String payload, @Header("MessageGroupId") String messageGroupId) {
        log.info("messageGroupId: {}", messageGroupId);
        FileReadyEvent fileReady = convertToObject(payload, FileReadyEvent.class);
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, fileReady.getDownloadUrl());

        MDCUtils.addMDCToContextAndExecute(Mono.just(fileReady))
                .filter(this::isFileReadyEvent)
                .flatMap(fileReadyEvent -> {
                    String downloadId = Utility.downloadId(fileReadyEvent);
                    return portFatDownloadDAO.findByDownloadId(downloadId)
                            .flatMap(portFatDownload -> {
                                if (portFatDownload.getStatus() == DownloadStatus.IN_PROGRESS
                                        || portFatDownload.getStatus() == DownloadStatus.COMPLETED) {
                                    log.info("Download {} for {}", portFatDownload.getStatus(), portFatDownload.getDownloadId());
                                    return Mono.empty();
                                }
                                return createAndSaveNewDownload(fileReadyEvent)
                                        .flatMap(portfatService::processZipFile);
                            });
                })
                .block();
    }

    private Mono<PortFatDownload> createAndSaveNewDownload(FileReadyEvent fileReadyEvent) {
        return Mono.just(portFatDownloadToEntity(fileReadyEvent));
    }

    private boolean isFileReadyEvent(FileReadyEvent fileReadyEvent) {
        String downloadUrl = fileReadyEvent.getDownloadUrl();
        boolean isFileReadyEvent = downloadUrl.startsWith(portFatConfig.getBlobStorageBaseUrl())
                && portFatConfig.getFilePathWhiteList().stream().anyMatch(downloadUrl::contains)
                && fileReadyEvent.getFileVersion() != null
                && !fileReadyEvent.getFileVersion().trim().isBlank();

        if (isFileReadyEvent) {
            log.info("The message received is valid {} ", fileReadyEvent);
        } else {
            log.error("The message received is not valid {}", fileReadyEvent);
        }
        return isFileReadyEvent;
    }

    private <T> T convertToObject(String body, Class<T> tClass) {
        T entity = Utility.jsonToObject(this.objectMapper, body, tClass);
        if (entity == null) throw new PnGenericException(MAPPER_ERROR, MAPPER_ERROR.getMessage());
        return entity;
    }

}
