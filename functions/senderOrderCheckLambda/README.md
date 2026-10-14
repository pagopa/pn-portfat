# Sender Order Check Lambda

Lambda function utilizzata per verificare la corretta acquisizione da parte di SEND delle commesse mensili rese disponibili da Portale Fatturazione.

La Lambda controlla che, per il mese corrente **X**, siano presenti record in stato `COMPLETED` relativi alle commesse dei mesi:

- **X+1**
- **X+4**

## Descrizione

La funzione Lambda esegue i seguenti controlli:

- determina il mese corrente **X**;
- utilizza il mese corrente come valore di `monthSent`;
- recupera da DynamoDB i record con:
    - `status = COMPLETED`;
    - `monthSent = X`;
- verifica che tra i record recuperati sia presente almeno un record con `orderMonth = X+1`;
- verifica che tra i record recuperati sia presente almeno un record con `orderMonth = X+4`;
- restituisce `OK` se entrambi i mesi attesi sono presenti;
- restituisce `KO` indicando i mesi mancanti se uno o entrambi non sono presenti;
- propaga eventuali errori generati durante l'accesso a DynamoDB.

È ammessa la presenza di più record relativi allo stesso `orderMonth`: affinché il controllo sia considerato valido 
è sufficiente che sia presente **almeno un record per X+1 e almeno un record per X+4**.

## Logica di controllo

Dato, ad esempio, il mese corrente:

```text
X = 2026-10
```

la Lambda calcola:

```text
X+1 = 2026-11
X+4 = 2027-02
```

La query DynamoDB recupera i record con:

```text
status = COMPLETED
monthSent = 2026-10
```

Successivamente, sui record restituiti, viene verificata la presenza di:

```text
orderMonth = 2026-11
orderMonth = 2027-02
```

Il controllo ha esito positivo solamente se entrambi i valori sono presenti.

## Variabili d'Ambiente

### `PORTFAT_DOWNLOAD_TABLE_NAME`

Nome della tabella DynamoDB contenente i record PortFatDownload.

Se la variabile non è valorizzata viene utilizzato il valore di default:

```text
pn-PortFatDownload
```

## Formato Mese

I campi `monthSent` e `orderMonth` utilizzano il formato:

```text
yyyy-MM
```

Esempio:

```text
2026-10
```

Il calcolo dei mesi viene effettuato tramite `YearMonth` della libreria `@js-joda/core`.

Il passaggio tra anni viene gestito automaticamente.

Ad esempio:

```text
X   = 2026-12
X+1 = 2027-01
X+4 = 2027-04
```

## Response Format

La Lambda non restituisce direttamente una response HTTP, ma un oggetto contenente l'esito del controllo e l'eventuale elenco dei mesi mancanti.

### Esito positivo

Quando sono presenti almeno un record per `X+1` e almeno un record per `X+4`:

```json
{
  "checkResult": "OK",
  "missingMonths": []
}
```

### Esito negativo - X+1 mancante

Esempio con:

```text
X   = 2026-10
X+1 = 2026-11
X+4 = 2027-02
```

se manca il record relativo a `X+1`:

```json
{
  "checkResult": "KO",
  "missingMonths": [
    "X+1 (2026-11)"
  ]
}
```

### Esito negativo - X+4 mancante

Se manca il record relativo a `X+4`:

```json
{
  "checkResult": "KO",
  "missingMonths": [
    "X+4 (2027-02)"
  ]
}
```

### Esito negativo - entrambi i mesi mancanti

Se non è presente nessuno dei due mesi attesi:

```json
{
  "checkResult": "KO",
  "missingMonths": [
    "X+1 (2026-11)",
    "X+4 (2027-02)"
  ]
}
```

## Gestione Errori

Gli errori generati durante l'esecuzione della query DynamoDB non vengono convertiti in un risultato `KO`.

L'errore viene propagato dalla Lambda in modo che l'infrastruttura AWS possa rile