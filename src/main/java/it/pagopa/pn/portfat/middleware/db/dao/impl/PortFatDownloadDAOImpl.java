package it.pagopa.pn.portfat.middleware.db.dao.impl;

import it.pagopa.pn.commons.db.BaseDAO;
import it.pagopa.pn.portfat.config.aws.AwsPropertiesConfig;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Slf4j
@Repository
public class PortFatDownloadDAOImpl extends BaseDAO<PortFatDownload> implements PortFatDownloadDAO {

    protected PortFatDownloadDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                     DynamoDbAsyncClient dynamoDbAsyncClient,
                                     AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, awsPropertiesConfig.getDynamodbPrtFatTable(), PortFatDownload.class);
    }

    @Override
    public Mono<PortFatDownload> findByDownloadId(String downloadId) {
        return Mono.fromFuture(this.dynamoTable.getItem(keyBuild(downloadId, null)).thenApply(item -> item));
    }

}
