CREATE TABLE service_quotes (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL REFERENCES service_requests(id) ON DELETE CASCADE,
    technician_id UUID NOT NULL REFERENCES users(id),
    price NUMERIC(12, 2) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(255) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_service_quotes_request_technician
        UNIQUE (service_request_id, technician_id)
);

CREATE INDEX idx_service_quotes_request_status
    ON service_quotes(service_request_id, status);

INSERT INTO service_quotes (
    id, service_request_id, technician_id, price, description, status, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    id,
    technician_id,
    technician_price,
    NULL,
    CASE WHEN status = 'QUOTE_ACCEPTED' THEN 'ACCEPTED' ELSE 'PENDING' END,
    created_at,
    NOW()
FROM service_requests
WHERE technician_id IS NOT NULL
  AND technician_price IS NOT NULL
  AND status IN ('QUOTED', 'QUOTE_ACCEPTED');

UPDATE service_requests
SET status = 'QUOTE_PENDING',
    technician_id = NULL,
    technician_price = NULL,
    final_price = NULL
WHERE status = 'QUOTED';
