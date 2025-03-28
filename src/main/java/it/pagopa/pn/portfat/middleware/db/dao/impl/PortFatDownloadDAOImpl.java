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

/**
 * Implementazione del Data Access Object (DAO) per la gestione delle operazioni su PortFatDownload.
 * Questa classe utilizza DynamoDB come database per memorizzare e recuperare informazioni sui download.
 */
@Slf4j
@Repository
public class PortFatDownloadDAOImpl extends BaseDAO<PortFatDownload> implements PortFatDownloadDAO {

    /**
     * Costruttore che inizializza il DAO con il client DynamoDB e la configurazione AWS.
     *
     * @param dynamoDbEnhancedAsyncClient client per operazioni asincrone su DynamoDB
     * @param dynamoDbAsyncClient         client asincrono per operazioni di basso livello su DynamoDB
     * @param awsPropertiesConfig         configurazione AWS contenente il nome della tabella DynamoDB
     */
    protected PortFatDownloadDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                     DynamoDbAsyncClient dynamoDbAsyncClient,
                                     AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, awsPropertiesConfig.getDynamodbPrtFatTable(), PortFatDownload.class);
    }

    /**
     * Recupera un'istanza di PortFatDownload dal database a partire dal downloadId.
     *
     * @param downloadId identificativo del download da cercare
     * @return un oggetto Mono contenente l'istanza di PortFatDownload, se trovata
     */
    @Override
    public Mono<PortFatDownload> findByDownloadId(String downloadId) {
        return Mono.fromFuture(this.get(downloadId, null).toFuture());
    }

    /**
     * Salva una nuova istanza di PortFatDownload nel database.
     *
     * @param portFatDownload l'istanza da salvare
     * @return un oggetto Mono contenente l'istanza salvata
     */
    @Override
    public Mono<PortFatDownload> createPortFatDownload(PortFatDownload portFatDownload) {
        return Mono.fromFuture(this.put(portFatDownload).toFuture());
    }

    /**
     * Aggiorna un'istanza esistente di PortFatDownload nel database.
     *
     * @param portFatDownload l'istanza da aggiornare con i nuovi dati
     * @return un oggetto Mono contenente l'istanza aggiornata
     */
    @Override
    public Mono<PortFatDownload> updatePortFatDownload(PortFatDownload portFatDownload) {
        return Mono.fromFuture(this.update(portFatDownload).toFuture());
    }

}
