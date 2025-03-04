# Event File Ready

## Descrizione
Questa repository contiene la Lambda `event-file-ready` che riceve eventi di file pronti dal portale di fatturazione e li invia a una coda SQS FIFO.

---

### Requisiti per lo Sviluppo Locale
- Node.js 20
- Docker (per i test di integrazione)
- LocalStack (gestito da testcontainers nei test)

### Installazione
```bash
npm install