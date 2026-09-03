-- The fraud service's own store: a durable log of every evaluation. Streaming
-- features live in Redis; this table holds decisions for evaluating the model
-- (especially in shadow mode) against real outcomes.

CREATE TABLE fraud_decisions (
    id                 BIGSERIAL PRIMARY KEY,
    decided_at         TIMESTAMPTZ NOT NULL,
    account_id         TEXT        NOT NULL,
    amount_minor       BIGINT      NOT NULL,
    currency           TEXT        NOT NULL,
    counterparty_id    TEXT,
    transaction_id     TEXT,
    effective_decision TEXT        NOT NULL,
    rules_decision     TEXT        NOT NULL,
    model_decision     TEXT        NOT NULL,
    model_score        DOUBLE PRECISION NOT NULL,
    mode               TEXT        NOT NULL,
    reasons            TEXT
);

CREATE INDEX idx_fraud_decisions_account ON fraud_decisions (account_id, decided_at DESC);
CREATE INDEX idx_fraud_decisions_shadow_disagreement
    ON fraud_decisions (decided_at DESC)
    WHERE mode = 'SHADOW' AND model_decision <> rules_decision;
