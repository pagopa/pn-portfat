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
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.portfat.middleware.db.converter.PortFatConverter.completed;
import static it.pagopa.pn.portfat.middleware.db.converter.PortFatConverter.portFatDownload;
import static it.pagopa.pn.portfat.utils.Utility.convertToObject;
import static it.pagopa.pn.portfat.utils.Utility.downloadId;

@Component
@CustomLog
@RequiredArgsConstructor
public class QueueListener {

    private final PortFatPropertiesConfig portFatConfig;
    private final PortFatService portfatService;
    private final PortFatDownloadDAO portFatDownloadDAO;
    private static final String MESSAGE_GROUP_ID = "MessageGroupId";

    @SqsListener(value = "${pn.portfat.sqsQueue}", deletionPolicy = SqsMessageDeletionPolicy.DEFAULT)
    public void pullPortFat(@Payload String payload, @Headers Map<String, Object> headers) {
        FileReadyEvent fileReady = convertToObject(payload, FileReadyEvent.class);
        setMDCContext(headers);
        log.logStartingProcess("portFat with MessageGroupId=" + headers.get(MESSAGE_GROUP_ID) + ", and Body= " + payload);
        MDCUtils.addMDCToContextAndExecute(Mono.just(fileReady))
                .filter(this::isFileReadyEvent)
                .flatMap(fileReadyEvent -> {
                    String downloadId = downloadId(fileReadyEvent);
                    log.info("Searching for downloadId: {}", downloadId);
                    return portFatDownloadDAO.findByDownloadId(downloadId)
                            .doOnNext(portFatDownload -> log.info("Found in DB: {}", portFatDownload))
                            .switchIfEmpty(Mono.defer(() -> {
                                log.info("No PortFatDownload found, creating a new one with IN_PROGRESS status");
                                return createAndSaveNewDownload(fileReadyEvent)
                                        .doOnNext(newPortFatDownload -> log.info("Creating new PortFatDownload with IN_PROGRESS status, DOWNLOAD_ID={}",
                                                newPortFatDownload.getDownloadId()))
                                        .flatMap(newPortFatDownload ->
                                                portfatService.processZipFile(newPortFatDownload)
                                                        .then(updateStatusToCompleted(newPortFatDownload))
                                                        .ignoreElement()
                                        );
                            }))
                            .flatMap(portFatDownload -> {
                                if (portFatDownload.getStatus() == DownloadStatus.ERROR) {
                                    log.info("PortFatDownload is in ERROR state, updating to IN_PROGRESS, DOWNLOAD_ID={}", portFatDownload.getDownloadId());
                                    portFatDownload.setStatus(DownloadStatus.IN_PROGRESS);
                                    portFatDownload.setUpdatedAt(Instant.now().toString());
                                    return portFatDownloadDAO.updatePortFatDownload(portFatDownload)
                                            .flatMap(portFatDownloadInProgress -> portfatService.processZipFile(portFatDownloadInProgress)
                                                    .then(updateStatusToCompleted(portFatDownloadInProgress))
                                            );
                                }
                                log.info("PortFatDownload is in {} state, DOWNLOAD_ID={}, doing nothing", portFatDownload.getStatus(), portFatDownload.getDownloadId());
                                return Mono.empty();
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error occurred during processing, updating status to ERROR", e);
                    return portFatDownloadDAO.findByDownloadId(downloadId(fileReady))
                            .flatMap(portFatDownload -> {
                                portFatDownload.setStatus(DownloadStatus.ERROR);
                                portFatDownload.setUpdatedAt(Instant.now().toString());
                                portFatDownload.setErrorMessage(e.getMessage());
                                return portFatDownloadDAO.updatePortFatDownload(portFatDownload)
                                        .doOnNext(error -> log.logEndingProcess("portFat STATUS= " + error.getStatus() + "DOWNLOAD_ID=" + error.getDownloadId()))
                                        .then(Mono.error(e));
                            });
                }).block();
    }

    private Mono<PortFatDownload> updateStatusToCompleted(PortFatDownload portFatDownload) {
        completed(portFatDownload);
        return portFatDownloadDAO.updatePortFatDownload(portFatDownload)
                .doOnNext(download ->
                        log.logEndingProcess("portFat updated To " + download.getStatus() + ", DOWNLOAD_ID=" + download.getDownloadId()));
    }

    private Mono<PortFatDownload> createAndSaveNewDownload(FileReadyEvent fileReadyEvent) {
        return portFatDownloadDAO.createPortFatDownload(portFatDownload(fileReadyEvent));
    }

    private boolean isFileReadyEvent(FileReadyEvent fileReadyEvent) {
        String downloadUrl = fileReadyEvent.getDownloadUrl();
        boolean isFileReadyEvent = downloadUrl != null
                && !downloadUrl.isBlank()
                && downloadUrl.startsWith(portFatConfig.getBlobStorageBaseUrl())
                && portFatConfig.getFilePathWhiteList().stream().anyMatch(downloadUrl::contains)
                && fileReadyEvent.getFileVersion() != null
                && !fileReadyEvent.getFileVersion().trim().isBlank();

        if (isFileReadyEvent) {
            log.info("The message received is valid {} ", fileReadyEvent);
        } else {
            log.error("The message received is not valid Message={}. Must satisfy the following: must start with={} and path contain one of ={}",
                    fileReadyEvent, portFatConfig.getBlobStorageBaseUrl(), portFatConfig.getFilePathWhiteList());
        }
        return isFileReadyEvent;
    }

    private void setMDCContext(Map<String, Object> headers) {
        MDCUtils.clearMDCKeys();

        if (headers.containsKey("id")) {
            String awsMessageId = headers.get("id").toString();
            MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, awsMessageId);
        }

        if (headers.containsKey("AWSTraceHeader")) {
            String traceId = headers.get("AWSTraceHeader").toString();
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        } else {
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, String.valueOf(UUID.randomUUID()));
        }
    }
}
