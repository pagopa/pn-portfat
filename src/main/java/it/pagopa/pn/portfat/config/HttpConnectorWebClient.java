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
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.file.Path;

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
    public Mono<Void> downloadFileAsByteArray(String url, Path fileOutput) {
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
                                .doFinally(signalType -> DataBufferUtils.release(buffer))
                                .then()
                )
                .doOnError(ex -> log.error("Error during file download or writing: {}", ex.getMessage()))
                .onErrorMap(ex -> new PnGenericException(DOWNLOAD_ZIP_ERROR, DOWNLOAD_ZIP_ERROR.getMessage() + ex.getMessage()))
                .then();
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
