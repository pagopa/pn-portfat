# pn-portfat Event File Ready

## Descrizione
Questa repository contiene la Lambda `event-file-ready` che riceve eventi di file pronti dal portale di fatturazione e li invia a una coda SQS FIFO.

---

### Requisiti per lo Sviluppo Locale
- Node.js 20
- Docker
- LocalStack

## Installazione
```bash
npm install
```

## Esecuzione build

Il comando di seguito genera uno zip nella directory build contenente tutte e sole le dipendenze necessarie all'ambiente di produzione

```bash
    npm run-script build
```

## Esecuzione test

Il comando di seguito permette di eseguire tutti i test previsti

```
    npm test
```

## Esecuzione codecoverage

Il comando di seguito permette di eseguire la code coverga dopo l'esecuizione dei test

```bash
    npm run-script coverage
```

## Esecuzione test, coverage, sonar e build

Il comando di seguito permette di eseguire la routine dei test per poi generare lo zip di build

```bash
    npm run-script test-build
```


## API
### API per la gestione dell'evento file pronto

L'API viene invocata dal portale di fatturazione quando un file è pronto per essere elaborato. L'evento trasporta l'URL di download del file e la sua versione. La Lambda `event-file-ready` riceve questi eventi, valida i dati e li inoltra a una coda SQS FIFO per la successiva elaborazione.


```sh
aws lambda invoke \
    --profile sso_pn-core-X \
    --function-name pn-portfat-eventFilReadyLambda \
    --payload '{ "downloadUrl": "https://pagopa.blob.core.windows.net", "fileVersion": "1.0.0" }' \
    response.json
```

**Payload:**

```Javascript
{
    downloadUrl: "https://pagopa.blob.core.windows.net"    // Url della risorsa 
    fileVersion: "1.0.0"                             // Versione del file zip
}
```

**Response:**

```Javascript
{
  statusCode: 200 | 202 | 400 | 404 | 500
  message: "message response"
}
```
**Status codes:**
-   **202** - Request accepted: il messaggio è stato accodato per una successiva elaborazione.
-   **400** - Invalid JSON: payload malformato o con dati mancanti.
-   **404** - Route not found: endpoint non esistente.
-   **500** - Internal error: errore di sistema.
