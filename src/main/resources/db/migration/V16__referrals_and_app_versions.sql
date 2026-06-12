CREATE TABLE referral_codes (
    id UUID PRIMARY KEY,
    technician_id UUID NOT NULL UNIQUE REFERENCES users(id),
    code VARCHAR(30) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE referral_registrations (
    id UUID PRIMARY KEY,
    referral_code_id UUID NOT NULL REFERENCES referral_codes(id),
    referrer_technician_id UUID NOT NULL REFERENCES users(id),
    referred_user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    referred_user_role VARCHAR(20) NOT NULL CHECK (referred_user_role IN ('CLIENT', 'TECHNICIAN')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('REGISTERED', 'QUALIFIED', 'REWARD_GRANTED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    qualified_at TIMESTAMPTZ,
    reward_granted_at TIMESTAMPTZ
);

CREATE TABLE referral_rewards (
    id UUID PRIMARY KEY,
    technician_id UUID NOT NULL REFERENCES users(id),
    referral_registration_id UUID NOT NULL UNIQUE REFERENCES referral_registrations(id),
    reward_type VARCHAR(40) NOT NULL CHECK (reward_type IN ('FREE_COMMISSION_SERVICE')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'USED', 'EXPIRED', 'CANCELLED')),
    source_service_request_id UUID REFERENCES service_requests(id),
    used_service_request_id UUID REFERENCES service_requests(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    used_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ
);

CREATE INDEX idx_referral_registrations_referrer ON referral_registrations(referrer_technician_id);
CREATE INDEX idx_referral_rewards_technician_status ON referral_rewards(technician_id, status);

INSERT INTO referral_codes (id, technician_id, code, active, created_at, updated_at)
SELECT gen_random_uuid(), u.id, 'TG-' || UPPER(SUBSTRING(REPLACE(u.id::text, '-', '') FROM 1 FOR 6)),
       TRUE, NOW(), NOW()
FROM users u
WHERE u.role = 'TECHNICIAN'
ON CONFLICT DO NOTHING;

ALTER TABLE payments ADD COLUMN commission_waived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE payments ADD COLUMN commission_waived_reason VARCHAR(255);
ALTER TABLE payments ADD COLUMN referral_reward_id UUID REFERENCES referral_rewards(id);

CREATE TABLE app_versions (
    id UUID PRIMARY KEY,
    platform VARCHAR(20) NOT NULL UNIQUE CHECK (platform IN ('ANDROID', 'IOS')),
    minimum_supported_version VARCHAR(30) NOT NULL,
    latest_version VARCHAR(30) NOT NULL,
    force_update BOOLEAN NOT NULL DEFAULT FALSE,
    update_url VARCHAR(1000) NOT NULL,
    message VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO app_versions (id, platform, minimum_supported_version, latest_version, force_update,
                          update_url, message, active)
VALUES
    (gen_random_uuid(), 'ANDROID', '1.0.0', '1.0.0', FALSE,
     'https://play.google.com/store/apps/details?id=com.tecngo.app',
     'Hay una nueva versión disponible de TecnGo.', TRUE),
    (gen_random_uuid(), 'IOS', '1.0.0', '1.0.0', FALSE,
     'https://apps.apple.com/',
     'Hay una nueva versión disponible de TecnGo.', TRUE);

INSERT INTO system_parameters (id, parameter_key, parameter_value, description, type, active, updated_at)
VALUES
    (gen_random_uuid(), 'REFERRAL_PROGRAM_ENABLED', 'true', 'Activa el programa de referidos', 'BOOLEAN', TRUE, NOW()),
    (gen_random_uuid(), 'REFERRAL_REQUIRED_RATING', '5', 'Calificación requerida para otorgar el beneficio', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'REFERRAL_REWARD_EXPIRATION_DAYS', '0', 'Días de vigencia del beneficio; 0 no expira', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'REFERRAL_REWARD_ONLY_IF_COMMISSION_GT_ZERO', 'true', 'No consume beneficios cuando la comisión es cero', 'BOOLEAN', TRUE, NOW()),
    (gen_random_uuid(), 'APP_VERSION_CHECK_ENABLED', 'true', 'Activa el control de versión móvil', 'BOOLEAN', TRUE, NOW()),
    (gen_random_uuid(), 'ANDROID_PACKAGE_NAME', 'com.tecngo.app', 'Identificador Android de TecnGo', 'STRING', TRUE, NOW())
ON CONFLICT (parameter_key) DO NOTHING;
