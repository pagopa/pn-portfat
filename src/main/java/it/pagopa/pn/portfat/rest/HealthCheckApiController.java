package it.pagopa.pn.portfat.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class HealthCheckApiController /*implements HealthCheckApi*/ {

   // @Override
    public Mono<ResponseEntity<Void>> status(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok().build());
    }
}
