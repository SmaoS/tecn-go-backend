CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT user_roles_role_check
        CHECK (role IN ('CLIENT', 'TECHNICIAN', 'VERIFIER', 'ADMIN'))
);

INSERT INTO user_roles (user_id, role)
SELECT id, role
FROM users
ON CONFLICT DO NOTHING;

ALTER TABLE users
    ADD COLUMN active_mode VARCHAR(30);

UPDATE users
SET active_mode = role
WHERE role IN ('CLIENT', 'TECHNICIAN')
  AND active_mode IS NULL;

ALTER TABLE users
    ADD CONSTRAINT users_active_mode_check
        CHECK (active_mode IS NULL OR active_mode IN ('CLIENT', 'TECHNICIAN'));

CREATE TABLE user_active_mode_audits (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    changed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    previous_mode VARCHAR(30),
    new_mode VARCHAR(30) NOT NULL,
    reason VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_active_mode_audits_previous_check
        CHECK (previous_mode IS NULL OR previous_mode IN ('CLIENT', 'TECHNICIAN')),
    CONSTRAINT user_active_mode_audits_new_check
        CHECK (new_mode IN ('CLIENT', 'TECHNICIAN'))
);

CREATE INDEX idx_user_roles_role_user
    ON user_roles(role, user_id);

CREATE INDEX idx_user_active_mode_audits_user_created
    ON user_active_mode_audits(user_id, created_at DESC);

INSERT INTO user_active_mode_audits (
    id,
    user_id,
    changed_by_user_id,
    previous_mode,
    new_mode,
    reason,
    created_at
)
SELECT
    gen_random_uuid(),
    id,
    NULL,
    NULL,
    active_mode,
    'LEGACY_ROLE_MIGRATION',
    CURRENT_TIMESTAMP
FROM users
WHERE active_mode IS NOT NULL;
