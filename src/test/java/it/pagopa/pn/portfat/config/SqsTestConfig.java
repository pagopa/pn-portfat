package it.pagopa.pn.portfat.config;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@TestConfiguration
public class SqsTestConfig {

    @Value("${aws.region-code}")
    private String regionCode;

    @Value("${aws.endpoint-url}")
    private String awsEndpointUrl;


    @Bean
    @Primary
    public AmazonSQSAsync amazonSQSAsync() {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        threadPoolExecutor.setKeepAliveTime(10, TimeUnit.SECONDS);

        return AmazonSQSAsyncClientBuilder.standard()
                .withExecutorFactory(() -> threadPoolExecutor)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsEndpointUrl, regionCode))
                .build();
    }
}