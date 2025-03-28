# Nome del Microservizio

[![Build Status](https://api.travis-ci.com/organization/repo.svg)](https://travis-ci.com/organization/repo)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Breve descrizione (1-2 frasi) dello scopo del microservizio.

## Panoramica

Descrizione più dettagliata del microservizio:
- Scopo e responsabilità
- Confini del servizio
- Dipendenze da altri servizi/sistemi

## Prerequisiti

- Elenco di software/tool necessari (con versioni se rilevanti)
    - Java 11+, Node.js 14+, Docker 20+, ecc.
- Configurazioni particolari richieste
- Credenziali o permessi speciali

## Installazione

### Ambiente Locale
```bash
    git clone https://github.com/organization/repo.git
    cd repo
    npm install
    # oppure
    mvn clean install
```

## Configurazione

Il microservizio utilizza i seguenti parametri di configurazione, gestibili tramite:

1. **Variabili d'ambiente** (preferite per deployment cloud)
2. **File di configurazione** (`application.yml`/`application.properties` per Spring Boot, `.env` per Node.js)
3. **Config Server** (se integrato con Spring Cloud Config)

### Configurazione Base

| Variabile Ambiente          | File Property                  | Default       | Obbligatorio | Descrizione                               |
|----------------------------|--------------------------------|---------------|--------------|-------------------------------------------|
| `SERVER_PORT`              | `server.port`                 | 8080          | No           | Porta del servizio                       |
| `DB_URL`                   | `spring.datasource.url`       | -             | Sì           | JDBC URL del database                    |
| `DB_USERNAME`              | `spring.datasource.username`  | -             | Sì           | Username del database                    |
| `DB_PASSWORD`              | `spring.datasource.password`  | -             | Sì           | Password del database                    |
| `LOG_LEVEL`                | `logging.level.root`          | INFO          | No           | DEBUG/INFO/WARN/ERROR                    |
| `JWT_SECRET`               | `app.jwt.secret`              | -             | Sì           | Secret per JWT encoding                  |

### Configurazione Avanzata

| Variabile Ambiente               | Default                       | Descrizione                               |
|----------------------------------|-------------------------------|-------------------------------------------|
| `CACHE_TTL`                      | 300000 (5 min)                | TTL cache in millisecondi                |
| `RABBITMQ_HOST`                  | localhost                     | Host per RabbitMQ                         |
| `MAX_API_RETRIES`                | 3                             | Tentativi chiamate HTTP fallite           |
| `FEATURE_FLAG_X_ENABLED`         | false                         | Abilita feature sperimentale X            |

### Esempi di Configurazione

**application.yml (Spring Boot):**
```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

app:
  jwt:
    secret: ${JWT_SECRET}
  cache:
    ttl: ${CACHE_TTL:300000}
```

## API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

Elenco degli endpoint principali:
- `POST /api/v1/orders` - Crea un nuovo ordine
- `GET /api/v1/orders/{id}` - Recupera un ordine

## Test

### Esecuzione Test
```bash
    npm test
    # oppure
    mvn test
```

## Esecuzione in locale
### comandi per avviare applicazione in locale