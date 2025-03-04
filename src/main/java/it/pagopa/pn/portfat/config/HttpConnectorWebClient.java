package it.pagopa.pn.portfat.config;

import it.pagopa.pn.portfat.exception.PnGenericException;
import lombok.CustomLog;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.file.Path;

import static it.pagopa.pn.portfat.exception.ExceptionTypeEnum.DOWNLOAD_ZIP_ERROR;

@Component
@CustomLog
public class HttpConnectorWebClient implements HttpConnector {

    private final WebClient webClient;

    public HttpConnectorWebClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public Mono<Void> downloadFileAsByteArray(String url, Path fileOutput) {
        log.info("Url to download: {}", url);
        Flux<DataBuffer> dataBufferFlux = webClient
                .get()
                .uri(URI.create(url))
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .doOnError(ex -> log.error("Error in WebClient", ex));

        return DataBufferUtils.write(dataBufferFlux, fileOutput)
                .doOnTerminate(() -> log.info("Download completed and saved to: {}", fileOutput))
                .onErrorMap(ex -> {
                    log.error("Error writing to file: {}", ex.getMessage());
                    return new PnGenericException(DOWNLOAD_ZIP_ERROR, DOWNLOAD_ZIP_ERROR.getMessage() + ex.getMessage());
                })
                .then();
    }


}
