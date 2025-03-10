package it.pagopa.pn.portfat.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

@Configuration
@ConfigurationProperties("pn.portfat")
@Getter
@Setter
@Import(SharedAutoConfiguration.class)
public class PortFatPropertiesConfig {

    private List<String> filePathWhiteList;
    private String blobStorageBaseUrl;
    private String basePathZipFiele;
    private String zipExtension;
    private String clientSafeStorageBasePath;
    private String safeStorageCxId;

}
