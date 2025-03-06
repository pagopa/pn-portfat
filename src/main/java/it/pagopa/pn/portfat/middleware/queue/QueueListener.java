package it.pagopa.pn.portfat.middleware.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
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
import static it.pagopa.pn.portfat.middleware.db.converter.PortFatConverter.completed;
import static it.pagopa.pn.portfat.middleware.db.converter.PortFatConverter.portFatDownload;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueListener {

    private final ObjectMapper objectMapper;
    private final PortFatPropertiesConfig portFatConfig;
    private final PortFatService portfatService;
    private final PortFatDownloadDAO portFatDownloadDAO;

    @SqsListener(value = "${pn.portfat.queue}", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void pullPortFat(@Payload String payload, @Header("MessageGroupId") String messageGroupId) {
        log.info("messageGroupId: {}", messageGroupId);
        FileReadyEvent fileReady = convertToObject(payload, FileReadyEvent.class);
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, fileReady.getDownloadUrl());

        MDCUtils.addMDCToContextAndExecute(Mono.just(fileReady))
                //.filter(this::isFileReadyEvent)
                .flatMap(fileReadyEvent -> {
                    String downloadId = Utility.downloadId(fileReadyEvent);
                    log.info("Searching for downloadId: {}", downloadId);
                    return portFatDownloadDAO.findByDownloadId(downloadId)
                            .doOnNext(portFatDownload -> log.info("Found in DB: {}", portFatDownload))
                            .switchIfEmpty(Mono.defer(() -> {
                                log.info("No PortFatDownload found, creating a new one with IN_PROGRESS status");
                                return createAndSaveNewDownload(fileReadyEvent)
                                        .doOnNext(newPortFatDownload -> log.info("Creating new PortFatDownload with IN_PROGRESS status"))
                                        .flatMap(newPortFatDownload ->
                                                portfatService.processZipFile(newPortFatDownload)
                                                        .then(updateStatusToCompleted(newPortFatDownload))
                                        );
                            }))
                            .flatMap(portFatDownload -> {
                                if (portFatDownload.getStatus() == DownloadStatus.ERROR) {
                                    log.info("PortFatDownload is in ERROR state, updating to IN_PROGRESS");
                                    portFatDownload.setStatus(DownloadStatus.IN_PROGRESS);
                                    portFatDownload.setUpdatedAt(Instant.now().toString());
                                    return portFatDownloadDAO.updatePortFatDownload(portFatDownload)
                                            .then(Mono.fromCallable(() -> portfatService.processZipFile(portFatDownload)))
                                            .then(updateStatusToCompleted(portFatDownload));
                                } else if (portFatDownload.getStatus() == DownloadStatus.IN_PROGRESS ||
                                        portFatDownload.getStatus() == DownloadStatus.COMPLETED) {
                                    log.info("PortFatDownload is in IN_PROGRESS or COMPLETED state, doing nothing");
                                    return Mono.empty();
                                } else {
                                    log.info("PortFatDownload is in a different state, continuing processing");
                                    return Mono.fromCallable(() -> portfatService.processZipFile(portFatDownload))
                                            .then(updateStatusToCompleted(portFatDownload));
                                }
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error occurred during processing, updating status to ERROR", e);
                    return portFatDownloadDAO.findByDownloadId(Utility.downloadId(fileReady))
                            .flatMap(portFatDownload -> {
                                if (portFatDownload != null) {
                                    portFatDownload.setStatus(DownloadStatus.ERROR);
                                    portFatDownload.setUpdatedAt(Instant.now().toString());
                                    return portFatDownloadDAO.updatePortFatDownload(portFatDownload)
                                            .then(Mono.error(e));
                                }
                                return Mono.error(e);
                            });
                }).subscribe();
    }

    private Mono<PortFatDownload> updateStatusToCompleted(PortFatDownload portFatDownload) {
        completed(portFatDownload);
        return portFatDownloadDAO.updatePortFatDownload(portFatDownload);
    }

    private Mono<PortFatDownload> createAndSaveNewDownload(FileReadyEvent fileReadyEvent) {
        return portFatDownloadDAO.createPortFatDownload(portFatDownload(fileReadyEvent));
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
