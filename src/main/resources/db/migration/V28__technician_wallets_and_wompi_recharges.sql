INSERT INTO system_parameters (id, parameter_key, parameter_value, description, type, active, updated_at) VALUES
    (gen_random_uuid(), 'TECHNICIAN_RECHARGE_ENABLED', 'false', 'Habilita o deshabilita el sistema de recargas para técnicos', 'BOOLEAN', TRUE, NOW()),
    (gen_random_uuid(), 'TECHNICIAN_BLOCK_WHEN_NEGATIVE_BALANCE', 'true', 'Bloquea cotizaciones si el saldo del técnico es menor a cero', 'BOOLEAN', TRUE, NOW()),
    (gen_random_uuid(), 'TECHNICIAN_LOW_BALANCE_WARNING_ENABLED', 'false', 'Muestra recomendación de recargar por saldo bajo', 'BOOLEAN', TRUE, NOW()),
    (gen_random_uuid(), 'TECHNICIAN_LOW_BALANCE_MINIMUM', '10000', 'Saldo mínimo para recomendar recarga', 'DECIMAL', TRUE, NOW()),
    (gen_random_uuid(), 'TECHNICIAN_MIN_RECHARGE_AMOUNT', '10000', 'Valor mínimo permitido de recarga técnica', 'DECIMAL', TRUE, NOW()),
    (gen_random_uuid(), 'TECHNICIAN_MAX_RECHARGE_AMOUNT', '500000', 'Valor máximo permitido de recarga técnica', 'DECIMAL', TRUE, NOW())
ON CONFLICT (parameter_key) DO NOTHING;

UPDATE system_parameters
SET parameter_value = '0',
    description = 'Porcentaje de comisión aplicado a pagos futuros',
    updated_at = NOW()
WHERE parameter_key = 'PLATFORM_COMMISSION_PERCENTAGE'
  AND parameter_value <> '0';

CREATE TABLE technician_wallets (
    id UUID PRIMARY KEY,
    technician_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance NUMERIC(12, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_technician_wallet_currency CHECK (currency = 'COP')
);

CREATE TABLE technician_wallet_transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES technician_wallets(id) ON DELETE CASCADE,
    technician_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL CHECK (type IN (
        'RECHARGE_PENDING',
        'RECHARGE_APPROVED',
        'RECHARGE_REJECTED',
        'COMMISSION_DEBIT',
        'COMMISSION_REFUND',
        'ADMIN_ADJUSTMENT'
    )),
    amount NUMERIC(12, 2) NOT NULL,
    balance_before NUMERIC(12, 2) NOT NULL,
    balance_after NUMERIC(12, 2) NOT NULL,
    reference VARCHAR(120),
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_wallet_transactions_technician_created
    ON technician_wallet_transactions(technician_id, created_at DESC);
CREATE UNIQUE INDEX uk_wallet_transactions_recharge_approved_reference
    ON technician_wallet_transactions(reference)
    WHERE type = 'RECHARGE_APPROVED' AND reference IS NOT NULL;

CREATE TABLE technician_recharges (
    id UUID PRIMARY KEY,
    technician_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    reference VARCHAR(120) NOT NULL UNIQUE,
    wompi_transaction_id VARCHAR(120) UNIQUE,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'CANCELLED')),
    payment_url VARCHAR(1200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    CONSTRAINT chk_technician_recharge_currency CHECK (currency = 'COP')
);

CREATE INDEX idx_technician_recharges_technician_created
    ON technician_recharges(technician_id, created_at DESC);

ALTER TABLE payments
    ADD COLUMN technician_wallet_transaction_id UUID REFERENCES technician_wallet_transactions(id);

INSERT INTO technician_wallets (id, technician_id, balance, currency, created_at, updated_at)
SELECT gen_random_uuid(), users.id, 0, 'COP', NOW(), NOW()
FROM users
WHERE users.role = 'TECHNICIAN'
ON CONFLICT (technician_id) DO NOTHING;
