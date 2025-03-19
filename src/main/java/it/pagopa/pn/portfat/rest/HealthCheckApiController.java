package it.pagopa.pn.portfat.rest;

import it.pagopa.pn.portfat.generated.openapi.server.v1.api.HealthCheckApi;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@AllArgsConstructor
public class HealthCheckApiController implements HealthCheckApi {

    private final HealthEndpoint healthEndpoint;

    @Override
    public Mono<ResponseEntity<Void>> status(final ServerWebExchange exchange) {
        if (healthEndpoint.health().getStatus().equals(Status.UP)) {
            return Mono.just(ResponseEntity.ok().build());
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }
}
