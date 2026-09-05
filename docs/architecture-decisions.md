# Architecture Decision Records

The load-bearing decisions behind the payments ledger, why each was made, and its
tradeoffs. Written to be read by an engineer evaluating the system.

## ADR-001: Money as integer minor units
**Decision.** All amounts are integer minor units (cents) end to end. No floating
point in the money path.
**Why.** Floating point cannot represent decimal money exactly; rounding drift is
unacceptable in a ledger.
**Tradeoff.** Callers work in minor units or convert at the edge; the API accepts
major-unit decimals and converts once, at the boundary.

## ADR-002: Transactional outbox over dual-write (and over 2PC)
**Decision.** The domain event is written to an outbox table in the same database
transaction as the state change; a polling relay publishes it to Kafka.
**Why.** A dual-write (DB then Kafka) can partially fail and leave the ledger and
the event log disagreeing. Two-phase commit couples availability across systems.
The outbox makes the event atomic with the state.
**Tradeoff.** At-least-once delivery, so consumers must deduplicate by event id.
Debezium/CDC is the alternative and is noted as a future option; the polling relay
is self-contained.

## ADR-003: Serializable isolation for money movement
**Decision.** Each posting runs in one SERIALIZABLE transaction, with automatic
retry on serialization conflicts.
**Why.** Correctness first: overdraft checks and balance updates must not race.
**Tradeoff.** Lower single-partition throughput than weaker isolation. Scaling is by
account-homed sharding (ADR-007), not by weakening isolation.

## ADR-004: Exactly-once money movement via idempotency keys
**Decision.** Clients send an idempotency key; the service stores a request
fingerprint. Same key and same fingerprint replays the stored result; same key with
a different fingerprint is a 409 conflict.
**Why.** Network retries must never double-post money.
**Tradeoff.** Clients must supply and manage keys.

## ADR-005: CP ledger, AP fraud, fail-open seam
**Decision.** The ledger is correctness-first (consistent). The fraud service is
availability-first. The ledger consults fraud inline with a strict timeout and fails
open (allows) on timeout or error.
**Why.** A fraud outage must never block money movement; the ledger's availability
cannot depend on fraud.
**Tradeoff.** A fraud outage means some transactions are unscreened; screening
catches up asynchronously off the event stream.

## ADR-006: Independent reconciliation
**Decision.** A separate service re-derives balances from the event stream and
cross-checks three independent derivations (stored balance, sum of postings, and
event-derived balance) plus global conservation.
**Why.** Silent balance corruption is the worst failure mode; an independent auditor
turns it into a measurable drift metric.
**Tradeoff.** Reconciliation reads the ledger database read-only (a read replica in
production), the one sanctioned cross-context read. It distinguishes consumer lag
from real drift.

## ADR-007: Active-passive multi-region (account-homed sharding)
**Decision.** The scaling and DR path is account-homed regional sharding (each
account owned by one region) with active-passive failover.
**Why.** A serializable ledger cannot be trivially active-active. Homing an account
to one region preserves per-account serializability while scaling horizontally.
**Tradeoff.** Cross-region account moves require an explicit migration; not
implemented, documented as the path.

## ADR-008: Fraud model in shadow mode by default
**Decision.** Screening combines deterministic rules with a logistic-regression
model. By default the model is scored and logged but does not enforce; only the rules
do. Activation is a config flag.
**Why.** The safe way to ship a model into a money path: observe it against outcomes
before giving it authority.
**Tradeoff.** In shadow mode the model provides no protection, only signal. That is
intentional for rollout.

## ADR-009: Versioned JSON event envelope over a schema registry
**Decision.** Events use a JSON envelope with an explicit schemaVersion; an AsyncAPI
document is the contract.
**Why.** Self-contained and adequate at this scale. A schema registry with Avro is
the production hardening step and needs its own running service.
**Tradeoff.** No enforced compatibility at publish time; the envelope is shaped so
migrating to Avro is mechanical.

## ADR-010: HS256 for development, RS256/JWKS for production
**Decision.** Local development validates HS256 tokens with a shared secret.
Production validates RS256 against a JWKS endpoint from a real identity provider.
**Why.** HS256 is a zero-dependency local convenience; RS256/JWKS is the real model.
**Tradeoff.** The switch is a configuration change, not a code change; the resource
server code is unchanged.

## ADR-011: RBAC by scope, ABAC by claim
**Decision.** OAuth2 scopes decide who may call what (RBAC). Token claims decide
whether a specific transaction may proceed: a per-token amount ceiling and an account
allowlist (ABAC).
**Why.** Coarse authority (scopes) plus fine, attribute-driven limits (claims).
**Tradeoff.** ABAC logic lives in the ledger and must stay in sync with token
issuance policy.

---

## Known limitations and non-production elements

These are deliberate and documented, not oversights:

- **Rate limiting is per-pod, in-memory.** It does not enforce a global limit across
  replicas. Production keys the token bucket in Redis (already in the stack) or
  enforces at the ingress/gateway.
- **Fraud model weights are illustrative, not trained.** The scoring and serving path
  is real; a trained model would be loaded, not hard-coded.
- **Local infrastructure is single-node.** Kafka (KRaft single node), PostgreSQL, and
  Redis run single-node locally; the Helm chart uses emptyDir volumes. Production uses
  managed, replicated equivalents with persistent storage.
- **Cloud Terraform is validated but gated.** The GKE and Cloud SQL configuration is
  written and `terraform validate`-clean but blocked by an accept-costs precondition
  so it cannot bill by accident. The free, verified deploy runs on a local kind
  cluster.
- **End-to-end browser tests (Playwright) are not yet implemented.** Automated
  coverage is framework-free unit checks, Testcontainers integration, HTTP smoke
  tests per phase, and a frontend Vitest suite in CI.
