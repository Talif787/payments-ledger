# Phase 2: Events and Reconciliation

Phase 2 turns the transactional outbox (written since Phase 1) into a durable
event stream, and adds an independent reconciliation service that continuously
proves the ledger is correct.

## The event pipeline

1. A transaction commits in the ledger service. In that same serializable
   transaction, an outbox row is written (Phase 1 behavior).
2. The relay, a background poller, claims unpublished rows with
   `SELECT ... FOR UPDATE SKIP LOCKED`, publishes each to Kafka, and marks them
   published, all in one transaction.
3. Consumers read the stream and act on it.

### Delivery semantics

Delivery is **at-least-once**. If the process dies after Kafka acknowledges a
send but before the DB commit that marks the row published, the row is
republished on restart. Every event therefore carries a unique `eventId`, and
consumers deduplicate by it to obtain **exactly-once effects**.

### Ordering

Events are keyed by `aggregateId`, so all events for a given account (or
transaction) land on the same partition and are ordered there. With a single
relay instance, global publish order follows the outbox id. Running multiple
relay instances preserves per-aggregate order only if the poll is sharded by
`hash(aggregateId)`; that sharding is noted as a scaling step, not implemented in
this phase.

### Why a polling relay and not Debezium

A polling relay is self-contained: it needs only the database and Kafka. Debezium
(log-based CDC) avoids polling but requires running Kafka Connect and a
replication slot on the primary. At this scale the polling relay is the simpler,
lower-operational-cost choice; the code isolates publication behind a port, so
swapping in CDC later does not touch the domain.

## The event envelope

Every message is a versioned JSON envelope:

    {
      "eventId": "uuid",
      "eventType": "ledger.transaction.posted.v1",
      "schemaVersion": 1,
      "aggregateId": "…",
      "occurredAt": "2026-01-15T12:00:00Z",
      "data": { … event-specific payload … }
    }

The full contract is in `ledger-service/asyncapi.yaml`.

### Schema evolution

JSON plus an explicit `schemaVersion` is deliberately simple. The production
hardening step is a schema registry (Confluent Schema Registry with Avro or
Protobuf), which enforces compatibility at publish time and encodes compactly.
The envelope is shaped so that migration is mechanical: `data` becomes the
Avro-encoded body and `schemaVersion` maps to a registry schema id.

## Reconciliation

The reconciliation service is a separate deployable that answers one question
continuously: **do independent derivations of the ledger's state agree?** It
checks three invariants every pass:

1. Each account's stored balance equals the sum of its own postings.
2. Each account's balance re-derived from the event stream equals the stored
   balance (for accounts the consumer has caught up on).
3. All stored balances net to zero across the book.

It reads the ledger database read-only (a read replica in production) for (1) and
(3), and consumes `ledger.transactions.v1` to build the event-derived balances
for (2), in its own database. It never writes ledger state.

A nonzero `reconciliation.discrepancies` gauge is the alarm: it means the ledger,
its postings, and the event stream have stopped agreeing, which is exactly the
class of silent corruption a money system must never ship.

### Endpoints

- `GET  /v1/reconciliation/latest` — most recent report (204 until the first run).
- `POST /v1/reconciliation/run` — run a pass on demand.
- `GET  /actuator/prometheus` — includes `reconciliation.discrepancies` and,
  on the ledger service, `ledger.outbox.backlog` and `ledger.outbox.published`.
