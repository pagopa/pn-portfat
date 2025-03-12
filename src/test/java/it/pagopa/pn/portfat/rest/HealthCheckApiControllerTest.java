package it.pagopa.pn.portfat.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthCheckApiControllerTest {

    @Mock
    private HealthEndpoint healthEndpoint;

    @Mock
    private ServerWebExchange exchange;

    @InjectMocks
    private HealthCheckApiController healthController;

    @Test
    void testStatus_HealthUp_ShouldReturnOk() {
        when(healthEndpoint.health()).thenReturn(Health.up().build());
        Mono<ResponseEntity<Void>> response = healthController.status(exchange);
        StepVerifier.create(response)
                .expectNext(ResponseEntity.ok().build())
                .verifyComplete();
    }

    @Test
    void testStatus_HealthDown_ShouldReturnInternalServerError() {
        when(healthEndpoint.health()).thenReturn(Health.down().build());
        Mono<ResponseEntity<Void>> response = healthController.status(exchange);
        StepVerifier.create(response)
                .expectNext(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                .verifyComplete();
    }

}