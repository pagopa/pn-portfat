package it.pagopa.pn.portfat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties("pn.pn-portfat")
@Getter
@Setter
public class PortfatPropertiesConfig {

    private List<String> filePathWhiteList;
    private String blobStorageBaseUrl;
    private String basePathZipFiele;

}
