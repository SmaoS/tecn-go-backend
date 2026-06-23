ALTER TABLE service_requests
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;

UPDATE service_requests
SET expires_at = created_at + INTERVAL '24 hours'
WHERE expires_at IS NULL;

ALTER TABLE service_requests
    ALTER COLUMN expires_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_service_requests_status_expires_at
    ON service_requests(status, expires_at);

INSERT INTO system_parameters (
    id, parameter_key, parameter_value, description, type, active, updated_at
)
VALUES (
    gen_random_uuid(),
    'SERVICE_REQUEST_EXPIRATION_HOURS',
    '24',
    'Horas que una solicitud permanece disponible antes de cancelarse automáticamente',
    'INTEGER',
    TRUE,
    NOW()
)
ON CONFLICT (parameter_key) DO NOTHING;
