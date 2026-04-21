package it.pagopa.pn.portfat.middleware.queue;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.portfat.config.HttpConnectorWebClient;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.exception.PnPortfatDownloadNotFoundException;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.DownloadStatus;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import it.pagopa.pn.portfat.service.PortFatService;
import it.pagopa.pn.portfat.service.SafeStorageService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.PORTFAT_DOWNLOAD_NOT_FOUND;
import static it.pagopa.pn.portfat.middleware.db.converter.PortFatConverter.completed;
import static it.pagopa.pn.portfat.utils.Utility.createDirectories;
import static it.pagopa.pn.portfat.utils.Utility.deleteTmpFiles;
import static it.pagopa.pn.portfat.utils.ZipUtility.unzip;

@Component
@CustomLog
@RequiredArgsConstructor
public class SafeStorageToPortfatQueueListener {

    private final PortFatPropertiesConfig portFatConfig;
    private final PortFatDownloadDAO portFatDownloadDAO;
    private final SafeStorageService safeStorageService;
    private final PortFatService portFatService;
    private final HttpConnectorWebClient webClient;

    private static final String TIME_FORMAT = "yyyy-MM-dd_HH-mm-ss-SSS";
    private static final String PATH_FILES = "port-fat-files";
    private static final String MESSAGE_GROUP_ID = "MessageGroupId";

    @SqsListener(value = "${pn.portfat.safeStorageQueue}", acknowledgementMode = SqsListenerAcknowledgementMode.ON_SUCCESS)
    public void safeStorageToPortfatConsumer(@Payload FileDownloadResponseDto payload, @Headers Map<String, Object> headers) {

        setMDCContext(headers);
        log.logStartingProcess("SafeStorageToPortfat with MessageGroupId=" + headers.get(MESSAGE_GROUP_ID) + ", Body=" + payload);

        String timestamp = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(TIME_FORMAT));
        String outputFilesPathStr = portFatConfig.getBasePathZipFile() + "_" + timestamp + "_" + PATH_FILES;
        Path outputFilesPath = createDirectories(outputFilesPathStr);
        String fileName = UUID.randomUUID().toString();
        Path zipFilePath = portFatService.createTmpFile(fileName, portFatConfig.getZipExtension());

        String fileKey = payload.getKey();
        Mono<Void> handledMessage = portFatDownloadDAO.findByArchiveFileKey(fileKey)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("No PortFatDownload found");
                    return Mono.error(new PnPortfatDownloadNotFoundException(PORTFAT_DOWNLOAD_NOT_FOUND, String.format(PORTFAT_DOWNLOAD_NOT_FOUND.getMessage(), fileKey)));
                }))
                .flatMap(portFatDownload -> retrieveAndProcessFile(fileKey, zipFilePath, outputFilesPath)
                        .thenReturn(portFatDownload)
                )
                .flatMap(this::updateStatusToCompleted)
                .onErrorResume(e -> {
                    log.error("Error occurred during processing", e);
                    return handleException(e, fileKey);
                })
                .doFinally(signalType -> {
                    deleteTmpFiles(zipFilePath);
                    deleteTmpFiles(outputFilesPath);
                })
                .then();

        MDCUtils.addMDCToContextAndExecute(handledMessage).block();
    }

    private Mono<PortFatDownload> handleException(Throwable e, String fileKey) {
        if (e instanceof PnPortfatDownloadNotFoundException) {
            return Mono.error(e);
        }
        return portFatDownloadDAO.findByArchiveFileKey(fileKey)
                .flatMap(portfatDownload -> updateDownloadToError(e, portfatDownload))
                .then(Mono.error(e));
    }

    private Mono<Void> retrieveAndProcessFile(String fileKey, Path zipFilePath, Path outputFilesPath) {
        return safeStorageService.callSafeStorageGetFile(fileKey)
                .flatMap(downloadURL -> webClient.downloadFileAsByteArray(downloadURL, zipFilePath))
                .then(unzip(zipFilePath.toString(), outputFilesPath.toString()))
                .flatMap(unused -> portFatService.processDirectory(outputFilesPath));
    }

    private Mono<PortFatDownload> updateDownloadToError(Throwable e, PortFatDownload download) {
        download.setStatus(DownloadStatus.ERROR);
        download.setUpdatedAt(Instant.now().toString());
        download.setErrorMessage(e.getMessage());
        return portFatDownloadDAO.updatePortFatDownload(download);
    }



    private void setMDCContext(Map<String, Object> headers) {
        MDCUtils.clearMDCKeys();

        if (headers.containsKey("id")) {
            MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, headers.get("id").toString());
        }

        if (headers.containsKey("X-Amzn-Trace-Id")) {
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, headers.get("X-Amzn-Trace-Id").toString());
        } else {
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, String.valueOf(UUID.randomUUID()));
        }
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

}