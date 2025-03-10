package it.pagopa.pn.portfat.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.portfat.config.PortFatPropertiesConfig;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.ApiClient;
import it.pagopa.pn.portfat.generated.openapi.msclient.pnsafestorage.v1.api.FileUploadApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SafeStorageReactiveConfiguration extends CommonBaseClient {

    @Bean
    public FileUploadApi templateReactiveConfiguration(PortFatPropertiesConfig cfg) {
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(cfg.getClientSafeStorageBasePath());
        return new FileUploadApi(apiClient);
    }

}
