package it.pagopa.pn.portfat.config;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.portfat.model.FileCreationWithContentRequest;
import lombok.CustomLog;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.DOWNLOAD_ZIP_ERROR;


/**
 * Implementazione del client HTTP per la gestione delle operazioni di download del file zip
 * e upload dei singoli file estratti dal zip.
 */
@Component
@CustomLog
public class HttpConnectorWebClient implements HttpConnector {

    private final WebClient webClient;

    public HttpConnectorWebClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    /**
     * Scarica un file da un URL e lo salva nel percorso specificato.
     *
     * @param url        l'URL del file da scaricare
     * @param fileOutput il percorso in cui salvare il file scaricato
     * @return un Mono che completa il download e il salvataggio del file
     */
    public Mono<Void> downloadFileAsByteArrayOld(String url, Path fileOutput) {
        log.info("Url to download zip: {}", url);
        return webClient.get()
                .uri(URI.create(url))
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .onStatus(HttpStatus::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error in WebClient during download: HTTP {} - {}", response.statusCode(), errorBody);
                                    return Mono.error(new PnGenericException(DOWNLOAD_ZIP_ERROR, "Error HTTP " + response.statusCode()));
                                })
                )
                .bodyToFlux(DataBuffer.class)
                .doOnNext(buffer -> log.info("Received buffer with {} bytes", buffer.readableByteCount()))
                .flatMap(buffer ->
                        DataBufferUtils.write(Mono.just(buffer), fileOutput)
                                .then()
                )
                .doOnError(ex -> log.error("Error during file download or writing: {}", ex.getMessage()))
                .onErrorMap(ex -> new PnGenericException(DOWNLOAD_ZIP_ERROR, DOWNLOAD_ZIP_ERROR.getMessage() + ex.getMessage()))
                .then();
    }

    public Mono<Void> downloadFileAsByteArray(String downloadUrl, Path path) {
        log.info("start to download file from: {}", downloadUrl);
        WritableByteChannel channel = null;
        try {
            channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            WritableByteChannel finalChannel = channel;
            return webClient
                    .get()
                    .uri(URI.create(downloadUrl))
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .onStatus(HttpStatus::isError, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Error in WebClient during download: HTTP {} - {}", response.statusCode(), errorBody);
                                        return Mono.error(new PnGenericException(DOWNLOAD_ZIP_ERROR, "Error HTTP " + response.statusCode()));
                                    })
                    )
                    .bodyToFlux(DataBuffer.class)
                    .flatMap(dataBuffer -> DataBufferUtils.write(Flux.just(dataBuffer), finalChannel)
                            .doOnError(e -> log.error("Error during file writing"))
                            .doFinally(signalType -> DataBufferUtils.release(dataBuffer)))
                    .doOnComplete(() -> closeWritableByteChannel(finalChannel))
                    .doOnError(throwable -> closeWritableByteChannel(finalChannel))
                    .then();

        } catch (Exception ex) {
            log.error("error in URI ", ex);
            closeWritableByteChannel(channel);
//            return Mono.error(new PaperEventEnricherException(ex.getMessage(), 500, "DOWNLOAD_ERROR"));
            return Mono.error(new PnGenericException(DOWNLOAD_ZIP_ERROR, DOWNLOAD_ZIP_ERROR.getMessage() + ex.getMessage()));
        }
    }

    public void closeWritableByteChannel(WritableByteChannel channel) {
        try {
            if(Objects.nonNull(channel) && channel.isOpen())
                channel.close();
            log.info("Download and file writing completed successfully");
        } catch (IOException e) {
            log.error("Error closing channel", e);
        }
    }

    /**
     * Carica il contenuto su Safe Storage remoto.
     *
     * @param fileCreationRequest  la richiesta contenente i dati da caricare
     * @param fileCreationResponse la risposta contenente i dettagli della destinazione del file
     * @param sha256               l'hash SHA-256 del contenuto da caricare
     * @return un Mono che completa l'upload del file
     */
    @Override
    public Mono<Void> uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponseDto fileCreationResponse, String sha256) {
        final String UPLOAD_FILE_CONTENT = "Safe Storage uploadContent";
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, UPLOAD_FILE_CONTENT, fileCreationResponse.getKey());

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-type", fileCreationRequest.getContentType());
        headers.add("x-amz-checksum-sha256", sha256);
        headers.add("x-amz-meta-secret", fileCreationResponse.getSecret());

        URI url = URI.create(fileCreationResponse.getUploadUrl());
        HttpMethod method = fileCreationResponse.getUploadMethod() == FileCreationResponseDto.UploadMethodEnum.POST ? HttpMethod.POST : HttpMethod.PUT;

        return webClient.method(method)
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .body(BodyInserters.fromResource(new ByteArrayResource(fileCreationRequest.getContent())))
                .retrieve()
                .toEntity(String.class)
                .flatMap(stringResponseEntity -> {
                    if (stringResponseEntity.getStatusCodeValue() != org.springframework.http.HttpStatus.OK.value()) {
                        return Mono.error(new RuntimeException());
                    }
                    return Mono.empty();
                });
    }

}
