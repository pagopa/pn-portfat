package it.pagopa.pn.portfat.config;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@AllArgsConstructor
public class S3Configuration {

    private final AwsPropertiesConfig awsConfigs;

    @Bean
    public AmazonS3 amazonS3() {
        if (StringUtils.hasText(awsConfigs.getEndpointUrl())) {
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsConfigs.getEndpointUrl(), awsConfigs.getRegionCode()))
                    .withPathStyleAccessEnabled(true)
                    .build();
        } else {
            return AmazonS3ClientBuilder.standard()
                    .withRegion(awsConfigs.getRegionCode())
                    .build();
        }
    }

}