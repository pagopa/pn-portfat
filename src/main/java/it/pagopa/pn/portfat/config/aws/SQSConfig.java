package it.pagopa.pn.portfat.config.aws;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@Slf4j
@AllArgsConstructor
public class SQSConfig {

    private final AwsPropertiesConfig awsConfigs;

    @Bean
    public AmazonSQSAsync amazonSQS() {
        log.info("SQS client region code: {}", awsConfigs.getRegionCode());
        if (StringUtils.hasText(awsConfigs.getEndpointUrl())) {
            log.info("SQS client endpoint: {}", awsConfigs.getEndpointUrl());
            return AmazonSQSAsyncClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsConfigs.getEndpointUrl(), awsConfigs.getRegionCode()))
                    .build();
        } else {
            return AmazonSQSAsyncClientBuilder.standard()
                    .withRegion(awsConfigs.getRegionCode())
                    .build();
        }
    }
}
