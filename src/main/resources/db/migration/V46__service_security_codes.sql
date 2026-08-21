ALTER TABLE service_requests DROP CONSTRAINT IF EXISTS service_requests_status_check;
ALTER TABLE service_requests
    ADD CONSTRAINT service_requests_status_check
    CHECK (status IN (
        'QUOTE_PENDING', 'QUOTED', 'QUOTE_ACCEPTED', 'ON_THE_WAY', 'ARRIVED',
        'SECURITY_VERIFIED', 'IN_PROGRESS', 'COMPLETED', 'PAID',
        'PAYMENT_DISPUTE', 'CANCELLED'
    ));

CREATE TABLE service_security_codes (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL REFERENCES service_requests(id) ON DELETE CASCADE,
    technician_id UUID NOT NULL REFERENCES users(id),
    client_id UUID NOT NULL REFERENCES users(id),
    code_plain VARCHAR(10) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('ACTIVE', 'VERIFIED', 'EXPIRED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    verification_attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL,
    regenerated_count INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_service_security_codes_active_request
    ON service_security_codes(service_request_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_service_security_codes_request_status
    ON service_security_codes(service_request_id, status);

CREATE TABLE service_security_audits (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL REFERENCES service_requests(id) ON DELETE CASCADE,
    technician_id UUID REFERENCES users(id),
    client_id UUID REFERENCES users(id),
    actor_user_id UUID REFERENCES users(id),
    action VARCHAR(40) NOT NULL CHECK (action IN (
        'SECURITY_CODE_GENERATED', 'SECURITY_CODE_REGENERATED',
        'SECURITY_CODE_VERIFIED', 'SECURITY_CODE_FAILED',
        'SECURITY_CODE_LOCKED', 'SECURITY_CODE_CANCELLED'
    )),
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_service_security_audits_request_created
    ON service_security_audits(service_request_id, created_at DESC);

INSERT INTO system_parameters (id, parameter_key, parameter_value, description, type, active, updated_at) VALUES
    (gen_random_uuid(), 'SERVICE_SECURITY_CODE_ENABLED', 'true', 'Exige código de seguridad antes de iniciar el servicio', 'BOOLEAN', TRUE, NOW()),
    (gen_random_uuid(), 'SERVICE_SECURITY_CODE_LENGTH', '6', 'Cantidad de dígitos del código de seguridad', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'SERVICE_SECURITY_CODE_EXPIRATION_MINUTES', '60', 'Minutos de vigencia del código de seguridad', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'SERVICE_SECURITY_MAX_ATTEMPTS', '5', 'Intentos máximos de verificación del código', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'SERVICE_SECURITY_MAX_REGENERATIONS', '3', 'Regeneraciones máximas del código por servicio', 'INTEGER', TRUE, NOW())
ON CONFLICT (parameter_key) DO NOTHING;
