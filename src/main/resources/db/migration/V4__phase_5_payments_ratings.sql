DO $$
BEGIN
IF to_regclass('public.service_requests') IS NULL THEN
    RETURN;
END IF;

CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL UNIQUE REFERENCES service_requests(id),
    client_id UUID NOT NULL REFERENCES users(id),
    technician_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(12, 2) NOT NULL,
    platform_fee NUMERIC(12, 2) NOT NULL,
    technician_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    method VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_payments_client_created
    ON payments(client_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_technician_created
    ON payments(technician_id, created_at DESC);

CREATE TABLE IF NOT EXISTS ratings (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL UNIQUE REFERENCES service_requests(id),
    client_id UUID NOT NULL REFERENCES users(id),
    technician_id UUID NOT NULL REFERENCES users(id),
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ratings_technician_created
    ON ratings(technician_id, created_at DESC);

ALTER TABLE service_requests DROP CONSTRAINT IF EXISTS service_requests_status_check;
ALTER TABLE service_requests
    ADD CONSTRAINT service_requests_status_check
    CHECK (status IN (
        'QUOTE_PENDING', 'QUOTED', 'QUOTE_ACCEPTED', 'ON_THE_WAY',
        'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'PAID', 'CANCELLED'
    ));

END;
$$;
