CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(80),
    aggregate_id VARCHAR(120),
    external_key VARCHAR(255),
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'DEAD')),
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    last_error VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_outbox_events_external_key
    ON outbox_events(external_key)
    WHERE external_key IS NOT NULL;

CREATE INDEX idx_outbox_events_dispatch
    ON outbox_events(status, available_at, created_at);

ALTER TABLE notifications
    ADD COLUMN outbox_event_id UUID;

CREATE UNIQUE INDEX uk_notifications_outbox_event
    ON notifications(outbox_event_id)
    WHERE outbox_event_id IS NOT NULL;

ALTER TABLE technician_recharges
    ADD COLUMN reconciliation_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_reconciliation_at TIMESTAMPTZ,
    ADD COLUMN last_reconciled_at TIMESTAMPTZ,
    ADD COLUMN last_reconciliation_error VARCHAR(1000);

CREATE INDEX idx_technician_recharges_reconciliation
    ON technician_recharges(status, next_reconciliation_at)
    WHERE status = 'PENDING' AND wompi_transaction_id IS NOT NULL;
