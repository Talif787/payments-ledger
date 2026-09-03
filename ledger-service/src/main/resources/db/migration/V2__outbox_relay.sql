-- Phase 2: prepare the outbox for the relay.
-- event_id is a stable, unique identity that travels with each event so
-- at-least-once delivery can be deduplicated by consumers. It is DB-generated,
-- so the Phase 1 insert path needs no change. gen_random_uuid() is built into
-- PostgreSQL 13+.

ALTER TABLE outbox ADD COLUMN event_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE outbox ADD CONSTRAINT uq_outbox_event_id UNIQUE (event_id);

-- Relay bookkeeping for observability and poison-message triage.
ALTER TABLE outbox ADD COLUMN attempts        INT NOT NULL DEFAULT 0;
ALTER TABLE outbox ADD COLUMN last_attempt_at TIMESTAMPTZ;
ALTER TABLE outbox ADD COLUMN last_error      TEXT;

-- FIFO claim index for the relay poll.
CREATE INDEX idx_outbox_relay_fifo ON outbox (id) WHERE published = FALSE;
