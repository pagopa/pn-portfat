package it.pagopa.pn.portfat.middleware.queue;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.DownloadStatus;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.model.FileReadyModel;
import it.pagopa.pn.portfat.service.PortFatService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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

/**
 * Listener per la coda SQS che gestisce i messaggi relativi ai file pronti per il download.
 * Questa classe riceve i messaggi dalla coda, verifica la validità dei dati,
 * e avvia il processo di download e gestione dello stato.
 */
@Component
@CustomLog
@RequiredArgsConstructor
public class QueueListener {

    private final PortFatPropertiesConfig portFatConfig;
    private final PortFatService portfatService;
    private final PortFatDownloadDAO portFatDownloadDAO;
    private static final String MESSAGE_GROUP_ID = "MessageGroupId";

    /**
     * Metodo che ascolta i messaggi dalla coda SQS e avvia il processo di download.
     *
     * @param payload il contenuto del messaggio ricevuto
     * @param headers gli header del messaggio SQS
     */
    @SqsListener(value = "${pn.portfat.sqsQueue}", deletionPolicy = SqsMessageDeletionPolicy.DEFAULT)
    public void pullPortFat(@Payload String payload, @Headers Map<String, Object> headers) {
        log.logStartingProcess("portFat with MessageGroupId=" + headers.get(MESSAGE_GROUP_ID) + ", and Body= " + payload);
        FileReadyModel fileReady = convertToObject(payload, FileReadyModel.class);
        setMDCContext(headers);
        var monoResult = Mono.just(fileReady)
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
                                                        .then(Mono.defer(() -> updateStatusToCompleted(newPortFatDownload)))
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
                                                    .then(Mono.defer(() -> updateStatusToCompleted(portFatDownloadInProgress)))
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
                                        .doOnNext(error -> log.logEndingProcess("portFat STATUS=" + error.getStatus() + " DOWNLOAD_ID=" + error.getDownloadId()))
                                        .then(Mono.error(e));
                            });
                });
        MDCUtils.addMDCToContextAndExecute(monoResult)
                .block();
    }

    /**
     * Aggiorna lo stato a COMPLETED una volta terminato il flusso.
     *
     * @param portFatDownload il download da aggiornare
     * @return il download aggiornato
     */
    private Mono<PortFatDownload> updateStatusToCompleted(PortFatDownload portFatDownload) {
        completed(portFatDownload);
        return portFatDownloadDAO.updatePortFatDownload(portFatDownload)
                .doOnNext(download ->
                        log.logEndingProcess("portFat updated To " + download.getStatus() + ", DOWNLOAD_ID=" + download.getDownloadId()));
    }

    /**
     * Crea e salva un nuovo entity relativa al flusso.
     *
     * @param fileReadyModel il modello contenente le informazioni del file
     * @return il nuovo record di download salvato
     */
    private Mono<PortFatDownload> createAndSaveNewDownload(FileReadyModel fileReadyModel) {
        return portFatDownloadDAO.createPortFatDownload(portFatDownload(fileReadyModel));
    }

    /**
     * Verifica se il messaggio ricevuto è valido.
     *
     * @param fileReadyModel il modello del file ricevuto
     * @return true se il messaggio è valido, false altrimenti
     */
    private boolean isFileReadyEvent(FileReadyModel fileReadyModel) {
        String downloadUrl = fileReadyModel.getDownloadUrl();
        String version = fileReadyModel.getFileVersion();
        String filePath = fileReadyModel.getFilePath();

        if (StringUtils.isEmpty(downloadUrl) || StringUtils.isEmpty(filePath)) {
            log.error("The message received is not valid Message={}, Download Url or FilePath are empty", fileReadyModel);
            return false;
        }

        if (!downloadUrl.startsWith(portFatConfig.getBlobStorageBaseUrl())) {
            log.error("The message received is not valid Message={}, Blob Storage Base Url is not valid", fileReadyModel);
            return false;
        }

        if (portFatConfig.getFilePathWhiteList().stream().noneMatch(downloadUrl::contains)
                && portFatConfig.getFilePathWhiteList().stream().noneMatch(filePath::contains)) {
            log.error("The message received is not valid Message={}, File Path is not valid", fileReadyModel);
            return false;
        }

        if (StringUtils.isEmpty(version)) {
            log.error("The message received is not valid Message={}, Version is empty", fileReadyModel);
            return false;
        }

        log.info("The message received is valid {} ", fileReadyModel);
        return true;
    }

    /**
     * Imposta il contesto MDC per il tracing dei log.
     *
     * @param headers gli header del messaggio SQS
     */
    private void setMDCContext(Map<String, Object> headers) {
        MDCUtils.clearMDCKeys();

        if (headers.containsKey("id")) {
            String awsMessageId = headers.get("id").toString();
            MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, awsMessageId);
        }

        if (headers.containsKey("X-Amzn-Trace-Id")) {
            String traceId = headers.get("X-Amzn-Trace-Id").toString();
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        } else {
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, String.valueOf(UUID.randomUUID()));
        }
    }
}
