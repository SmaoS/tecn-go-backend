ALTER TABLE users
    ADD COLUMN home_address VARCHAR(255),
    ADD COLUMN home_latitude DOUBLE PRECISION,
    ADD COLUMN home_longitude DOUBLE PRECISION,
    ADD COLUMN home_city VARCHAR(120),
    ADD COLUMN home_neighborhood VARCHAR(120);

CREATE TABLE system_parameters (
    id UUID PRIMARY KEY,
    parameter_key VARCHAR(100) NOT NULL UNIQUE,
    parameter_value VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(30) NOT NULL CHECK (type IN ('INTEGER', 'DECIMAL', 'BOOLEAN', 'STRING')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO system_parameters (id, parameter_key, parameter_value, description, type, active, updated_at) VALUES
    (gen_random_uuid(), 'QUOTE_EXPIRATION_MINUTES', '10', 'Minutos antes de expirar una cotización pendiente', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'PLATFORM_COMMISSION_PERCENTAGE', '10', 'Porcentaje de comisión aplicado a pagos futuros', 'DECIMAL', TRUE, NOW()),
    (gen_random_uuid(), 'TECHNICIAN_OFFLINE_AFTER_MINUTES', '3', 'Minutos sin actualización para considerar al técnico desconectado', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'LOCATION_POLLING_SECONDS', '10', 'Frecuencia sugerida de actualización GPS', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'SERVICE_POLLING_SECONDS', '10', 'Frecuencia sugerida de actualización de solicitudes', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'MAX_SERVICE_REQUEST_IMAGES', '5', 'Máximo de imágenes por solicitud', 'INTEGER', TRUE, NOW())
ON CONFLICT (parameter_key) DO NOTHING;

CREATE TABLE technician_locations (
    id UUID PRIMARY KEY,
    technician_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    online BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE service_quotes DROP CONSTRAINT IF EXISTS uk_service_quotes_request_technician;
ALTER TABLE service_quotes DROP CONSTRAINT IF EXISTS service_quotes_status_check;
ALTER TABLE service_quotes
    ADD COLUMN expires_at TIMESTAMPTZ,
    ADD COLUMN responded_at TIMESTAMPTZ,
    ADD CONSTRAINT service_quotes_status_check
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED'));

UPDATE service_quotes
SET expires_at = created_at + INTERVAL '10 minutes'
WHERE expires_at IS NULL;

ALTER TABLE service_quotes ALTER COLUMN expires_at SET NOT NULL;

CREATE UNIQUE INDEX uk_service_quotes_pending_request_technician
    ON service_quotes(service_request_id, technician_id)
    WHERE status = 'PENDING';

ALTER TABLE payments
    ADD COLUMN platform_commission_percentage NUMERIC(5, 2);

UPDATE payments
SET platform_commission_percentage =
    CASE WHEN amount = 0 THEN 0 ELSE ROUND((platform_fee / amount) * 100, 2) END
WHERE platform_commission_percentage IS NULL;

ALTER TABLE payments ALTER COLUMN platform_commission_percentage SET NOT NULL;

CREATE TABLE service_request_images (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL REFERENCES service_requests(id) ON DELETE CASCADE,
    image_url VARCHAR(1000) NOT NULL,
    public_id VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_service_request_images_request
    ON service_request_images(service_request_id, created_at);
