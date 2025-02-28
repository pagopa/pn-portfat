package it.pagopa.pn.portfat.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@AllArgsConstructor
public class HttpGenericClient {

    /**
     * Build bean RestTemplate.
     *
     * @return bean o restTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        var requestFactory = new HttpComponentsClientHttpRequestFactory();
        var factory = new BufferingClientHttpRequestFactory(requestFactory);
        return new RestTemplate(factory);
    }

}
