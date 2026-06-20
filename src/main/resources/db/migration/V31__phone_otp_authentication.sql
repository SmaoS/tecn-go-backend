ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL,
    ADD COLUMN phone_normalized VARCHAR(20);

CREATE UNIQUE INDEX uk_users_phone_normalized
    ON users(phone_normalized)
    WHERE phone_normalized IS NOT NULL;

CREATE TABLE phone_otp_verifications (
    id UUID PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    code_hash VARCHAR(100),
    provider VARCHAR(30) NOT NULL,
    provider_reference VARCHAR(255),
    request_ip_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    verification_token_hash VARCHAR(64),
    consumed_at TIMESTAMPTZ
);

CREATE INDEX idx_phone_otp_phone_created
    ON phone_otp_verifications(phone, created_at DESC);

CREATE INDEX idx_phone_otp_ip_created
    ON phone_otp_verifications(request_ip_hash, created_at DESC);

INSERT INTO system_parameters (
    id, parameter_key, parameter_value, description, type, active, updated_at
) VALUES
    (gen_random_uuid(), 'OTP_EXPIRATION_MINUTES', '5', 'Minutos de vigencia del código OTP', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'OTP_LENGTH', '5', 'Cantidad de dígitos del código OTP', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'OTP_MAX_ATTEMPTS', '5', 'Intentos máximos para validar un código OTP', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'OTP_MAX_SENDS_PER_PHONE', '3', 'Envíos OTP permitidos por teléfono en diez minutos', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'OTP_MAX_SENDS_PER_IP', '10', 'Envíos OTP permitidos por IP en diez minutos', 'INTEGER', TRUE, NOW())
ON CONFLICT (parameter_key) DO NOTHING;
