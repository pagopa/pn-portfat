# Sender Order Check Lambda

Lambda function per verificare la presenza di record COMPLETED per i mesi X+1 e X+4, dove X è il mese corrente.

## Descrizione

Questa funzione Lambda:
- Calcola il mese corrente (X)
- Verifica la presenza di almeno un record con `status = COMPLETED` e `orderMonth = X+1`
- Verifica la presenza di almeno un record con `status = COMPLETED` e `orderMonth = X+4`
- Termina con esito positivo (200) se entrambi i controlli hanno esito positivo
- Termina con esito negativo (400) se manca almeno uno dei due mesi attesi
- Restituisce errore (500) in caso di problemi durante l'esecuzione

## Variabili d'Ambiente

- `PORTFAT_DOWNLOAD_TABLE_NAME`: Nome della tabella DynamoDB PortFatDownload (default: `pn-PortFatDownload`)
- `AWS_REGION`: Regione AWS (default: `eu-south-1`)

## Formato Mese

Il formato utilizzato per `orderMonth` è `YYYY_MM` (es: `2026_10`)

## Response Format

### Esito Positivo (200)
```json
{
  "success": true,
  "message": "Check PASSED: Found COMPLETED records for both X+1 and X+4 months",
  "currentMonth": "2026_10",
  "nextMonth": "2026_11",
  "fourthMonth": "2027_02",
  "hasNextMonthRecord": true,
  "hasFourthMonthRecord": true,
  "timestamp": "2026-10-07T10:30:00.000Z"
}
```

### Esito Negativo (400)
```json
{
  "success": false,
  "message": "Check FAILED: Missing COMPLETED records. X+1 (2026_11): OK, X+4 (2027_02): MISSING",
  "currentMonth": "2026_10",
  "nextMonth": "2026_11",
  "fourthMonth": "2027_02",
  "hasNextMonthRecord": true,
  "hasFourthMonthRecord": false,
  "timestamp": "2026-10-07T10:30:00.000Z"
}
```

## Scheduling

Questa Lambda è schedulata per l'esecuzione ogni 22 del mese.

## Query DynamoDB

La funzione utilizza il Global Secondary Index `statusOrderMonth-index` sulla tabella `pn-PortFatDownload` per eseguire query efficienti:

```
IndexName: statusOrderMonth-index
PartitionKey: status (vale "COMPLETED")
SortKey: orderMonth (es: "2026_11", "2027_02")
```

## Installazione

```bash
npm install
```

## Test

```bash
npm test
```