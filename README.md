# Payments Ledger

A correctness-first, double-entry payments ledger with exactly-once money
movement. This repository is Phase 1 of a larger system: the ledger core. Later
phases add the event-log relay and reconciliation, fraud detection, security and
observability hardening, and the full deployment stack.

## What Phase 1 does

- Opens accounts and reads balances.
- Posts balanced double-entry transactions: within a transaction, total credits
  equal total debits, so money is conserved.
- Guarantees exactly-once money movement with client-supplied idempotency keys.
- Enforces an overdraft policy per account.
- Writes a transactional outbox row in the same database transaction as each
  state change, so downstream publication (Phase 2) is exactly-once.
- Runs every money-movement operation in a single SERIALIZABLE transaction with
  automatic retry on serialization conflicts.

## Design in one paragraph

The domain and application layers are framework-free and hold all the rules: the
`LedgerTransaction` aggregate cannot be constructed unless its postings balance,
`Money` is exact integer minor units (never floating point), and
`PostTransactionService` performs the idempotency check, row-locked balance read,
overdraft check, and atomic persistence of the transaction, balances, outbox
event, and idempotency record. The infrastructure layer adapts those rules to
PostgreSQL with plain JDBC (no ORM, so locking and transactions are explicit),
and the presentation layer maps HTTP to use cases and domain errors to status
codes. The database adds one more safety net: a deferred constraint trigger that
rejects any transaction whose postings do not net to zero.

## Requirements

- JDK 21
- Maven 3.9+
- Docker (for local Postgres and for the integration test)

## Run locally

```
docker compose up --build
```

The service listens on `http://localhost:8080`. Interactive API docs are at
`/swagger-ui.html`; health is at `/actuator/health`.

## Try it

```
# Open a funding account (overdraft allowed) and two user accounts
HOUSE=$(curl -s localhost:8080/v1/accounts -H 'Content-Type: application/json' \
  -d '{"currency":"USD","allowOverdraft":true}' | jq -r .accountId)
ALICE=$(curl -s localhost:8080/v1/accounts -H 'Content-Type: application/json' \
  -d '{"currency":"USD","allowOverdraft":false}' | jq -r .accountId)
BOB=$(curl -s localhost:8080/v1/accounts -H 'Content-Type: application/json' \
  -d '{"currency":"USD","allowOverdraft":false}' | jq -r .accountId)

# Fund Alice with 100.00
curl -s localhost:8080/v1/transactions \
  -H 'Content-Type: application/json' -H "Idempotency-Key: fund-alice-1" \
  -d "{\"postings\":[
        {\"accountId\":\"$HOUSE\",\"direction\":\"DEBIT\",\"amount\":\"100.00\",\"currency\":\"USD\"},
        {\"accountId\":\"$ALICE\",\"direction\":\"CREDIT\",\"amount\":\"100.00\",\"currency\":\"USD\"}]}"

# Transfer 10.00 Alice -> Bob (safe to retry with the same key)
curl -s localhost:8080/v1/transactions \
  -H 'Content-Type: application/json' -H "Idempotency-Key: transfer-1" \
  -d "{\"postings\":[
        {\"accountId\":\"$ALICE\",\"direction\":\"DEBIT\",\"amount\":\"10.00\",\"currency\":\"USD\"},
        {\"accountId\":\"$BOB\",\"direction\":\"CREDIT\",\"amount\":\"10.00\",\"currency\":\"USD\"}]}"

curl -s localhost:8080/v1/accounts/$ALICE/balance
```

## Test

```
cd ledger-service
mvn verify
```

Unit tests cover the domain and the application services with in-memory fakes.
The integration test starts a real PostgreSQL via Testcontainers and exercises
the full stack including the migration and the balanced-transaction trigger, so
it requires Docker.

## Project layout

```
ledger-service/src/main/java/com/ledger
  domain/         framework-free entities, value objects, invariants, events
  application/    use cases and the ports they depend on
  infrastructure/ JDBC adapters, transaction runner, id generator, config
  presentation/   REST controllers, DTOs, error mapping, correlation filter
  resources/      application.yml and Flyway migrations
```

## Conventions

- Credit-positive: a CREDIT increases an account balance, a DEBIT decreases it.
- One currency per transaction (multi-currency FX is out of scope for Phase 1).
- Money crosses the API as exact decimal strings and is stored as integer minor
  units.

## Roadmap

- Phase 2: outbox relay to Kafka, schema registry, reconciliation engine.
- Phase 3: fraud service (streaming features, rules plus model, inline decision).
- Phase 4: security (OAuth2/OIDC, RBAC/ABAC, rate limiting) and observability
  (OpenTelemetry tracing, Prometheus metrics, dashboards).
- Phase 5: Kubernetes, Helm, Terraform, CI/CD, and operational runbooks.
