CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,
    revoke_reason VARCHAR(120),
    ip_hash VARCHAR(64),
    user_agent VARCHAR(500),
    mfa_verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_auth_sessions_user_active
    ON auth_sessions(user_id, expires_at DESC)
    WHERE revoked_at IS NULL;

CREATE TABLE admin_mfa_challenges (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    challenge_token_hash VARCHAR(64) NOT NULL UNIQUE,
    code_hash VARCHAR(100) NOT NULL,
    request_ip_hash VARCHAR(64),
    attempts INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_mfa_user_created
    ON admin_mfa_challenges(user_id, created_at DESC);

CREATE TABLE security_rate_limit_events (
    id UUID PRIMARY KEY,
    action VARCHAR(60) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_security_rate_limit_action_key_created
    ON security_rate_limit_events(action, key_hash, created_at DESC);
