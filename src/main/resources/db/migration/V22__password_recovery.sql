CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_password_reset_tokens_user_active
    ON password_reset_tokens(user_id, used, expires_at DESC);

CREATE TABLE password_security_audits (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_password_security_audits_user_created
    ON password_security_audits(user_id, created_at DESC);
