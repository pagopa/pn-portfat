package it.pagopa.pn.portfat.config.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

@Configuration
@Slf4j
@AllArgsConstructor
public class SQSConfig {

    private final AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
    private final AwsRegionProvider regionProvider;

    @Bean
    AmazonSQSAsync amazonSQS() {
        return AmazonSQSAsyncClientBuilder.standard().withCredentials(credentialsProvider)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:4566","us-east-1"))
                .withRegion(regionProvider.getRegion().id()).build();
    }
}
