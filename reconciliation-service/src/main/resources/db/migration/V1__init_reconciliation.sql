-- Reconciliation service's own store. It never writes ledger state; these tables
-- hold only the event-derived projection, the consumer dedup log, and run history.

CREATE TABLE consumed_events (
    event_id    UUID PRIMARY KEY,
    consumed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE derived_balances (
    account_id  TEXT PRIMARY KEY,
    minor_units BIGINT NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reconciliation_runs (
    id                BIGSERIAL PRIMARY KEY,
    run_at            TIMESTAMPTZ NOT NULL,
    accounts_checked  INT NOT NULL,
    discrepancy_count INT NOT NULL,
    status            TEXT NOT NULL
);

CREATE INDEX idx_reconciliation_runs_run_at ON reconciliation_runs (run_at DESC);
