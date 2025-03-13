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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.DOWNLOAD_ZIP_ERROR;

@Component
@CustomLog
public class HttpConnectorWebClient implements HttpConnector {

    private final WebClient webClient;

    public HttpConnectorWebClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public Mono<Void> downloadFileAsByteArray(String url, Path fileOutput) {
        log.info("Url to download zip: {}", url);
        Flux<DataBuffer> dataBufferFlux = webClient
                .get()
                .uri(URI.create(url))
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .doOnError(ex -> log.error("Error in WebClient", ex));

        return DataBufferUtils.join(dataBufferFlux)
                .flatMap(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);

                    String base64String = new String(bytes, StandardCharsets.UTF_8);
                    byte[] decodedBytes = Base64.getDecoder().decode(base64String);

                    return Mono.fromCallable(() -> {
                        Files.write(fileOutput, decodedBytes);
                        return fileOutput;
                    }).subscribeOn(Schedulers.boundedElastic());
                })
                .doOnTerminate(() -> log.info("Download completed and saved to: {}", fileOutput))
                .onErrorMap(ex -> {
                    log.error("Error writing to file: {}", ex.getMessage());
                    return new PnGenericException(DOWNLOAD_ZIP_ERROR, DOWNLOAD_ZIP_ERROR.getMessage() + ex.getMessage());
                })
                .then();
    }

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
