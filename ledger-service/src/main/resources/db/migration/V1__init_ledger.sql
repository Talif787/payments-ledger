-- Ledger schema. Money is stored as signed integer minor units. Postings are
-- immutable and append-only; balances are a consistent projection guarded by an
-- optimistic version. A deferred constraint trigger enforces the double-entry
-- invariant (credits equal debits per transaction) at the database level as
-- defense in depth behind the domain's own guarantee.

CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    currency        TEXT        NOT NULL,
    status          TEXT        NOT NULL CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    allow_overdraft BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE account_balances (
    account_id  UUID PRIMARY KEY REFERENCES accounts (id),
    minor_units BIGINT      NOT NULL,
    currency    TEXT        NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
    id         UUID PRIMARY KEY,
    currency   TEXT        NOT NULL,
    metadata   JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE postings (
    id             BIGSERIAL PRIMARY KEY,
    transaction_id UUID        NOT NULL REFERENCES transactions (id),
    account_id     UUID        NOT NULL REFERENCES accounts (id),
    direction      TEXT        NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    minor_units    BIGINT      NOT NULL CHECK (minor_units > 0),
    currency       TEXT        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_postings_account_created ON postings (account_id, created_at);
CREATE INDEX idx_postings_transaction ON postings (transaction_id);

CREATE TABLE idempotency_keys (
    key                 TEXT PRIMARY KEY,
    request_fingerprint TEXT        NOT NULL,
    transaction_id      UUID        NOT NULL REFERENCES transactions (id),
    status              TEXT        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL
);

CREATE TABLE outbox (
    id           BIGSERIAL PRIMARY KEY,
    aggregate_id TEXT        NOT NULL,
    event_type   TEXT        NOT NULL,
    payload      JSONB       NOT NULL,
    published    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox (created_at) WHERE published = FALSE;

-- Defense in depth: verify that every transaction's postings net to zero.
CREATE OR REPLACE FUNCTION assert_transaction_balanced()
    RETURNS TRIGGER AS $$
DECLARE
    net BIGINT;
BEGIN
    SELECT COALESCE(SUM(CASE direction WHEN 'CREDIT' THEN minor_units ELSE -minor_units END), 0)
    INTO net
    FROM postings
    WHERE transaction_id = NEW.transaction_id;

    IF net <> 0 THEN
        RAISE EXCEPTION 'Unbalanced transaction %: net minor units = %', NEW.transaction_id, net;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_postings_balanced
    AFTER INSERT ON postings
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE FUNCTION assert_transaction_balanced();
