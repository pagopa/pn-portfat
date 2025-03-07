package it.pagopa.pn.portfat.middleware.queue;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.generated.openapi.server.v1.dto.FileReadyEvent;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.DownloadStatus;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.service.PortFatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static it.pagopa.pn.portfat.middleware.db.converter.PortFatConverter.completed;
import static it.pagopa.pn.portfat.middleware.db.converter.PortFatConverter.portFatDownload;
import static it.pagopa.pn.portfat.utils.Utility.convertToObject;
import static it.pagopa.pn.portfat.utils.Utility.downloadId;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueListener {

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
                    String downloadId = downloadId(fileReadyEvent);
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
                                                        .ignoreElement()
                                        );
                            }))
                            .flatMap(portFatDownload -> {
                                if (portFatDownload.getStatus() == DownloadStatus.ERROR) {
                                    log.info("PortFatDownload is in ERROR state, updating to IN_PROGRESS");
                                    portFatDownload.setStatus(DownloadStatus.IN_PROGRESS);
                                    portFatDownload.setUpdatedAt(Instant.now().toString());
                                    return portFatDownloadDAO.updatePortFatDownload(portFatDownload)
                                            .flatMap(portFatDownloadInProgress -> portfatService.processZipFile(portFatDownloadInProgress)
                                                    .then(updateStatusToCompleted(portFatDownloadInProgress))
                                            );
                                }
                                log.info("PortFatDownload is in {} state, doing nothing", portFatDownload.getStatus());
                                return Mono.empty();
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error occurred during processing, updating status to ERROR", e);
                    return portFatDownloadDAO.findByDownloadId(downloadId(fileReady))
                            .flatMap(portFatDownload -> {
                                if (portFatDownload != null) {
                                    portFatDownload.setStatus(DownloadStatus.ERROR);
                                    portFatDownload.setUpdatedAt(Instant.now().toString());
                                    portFatDownload.setErrorMessage(e.getMessage());
                                    return portFatDownloadDAO.updatePortFatDownload(portFatDownload)
                                            .then(Mono.error(e));
                                }
                                return Mono.error(e);
                            });
                }).block();
    }

    private Mono<PortFatDownload> updateStatusToCompleted(PortFatDownload portFatDownload) {
        completed(portFatDownload);
        return portFatDownloadDAO.updatePortFatDownload(portFatDownload)
                .doOnNext(portFatDownloadUpdated -> log.info("updated To Completed {}", portFatDownloadUpdated.getStatus()));
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

}
