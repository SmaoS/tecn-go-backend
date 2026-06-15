ALTER TABLE chat_messages
    ADD COLUMN moderation_status VARCHAR(20),
    ADD COLUMN moderation_reason VARCHAR(1000),
    ADD COLUMN moderated_at TIMESTAMPTZ,
    ADD COLUMN moderated_by_user_id UUID REFERENCES users(id);

UPDATE chat_messages
SET moderation_status = 'APPROVED',
    moderation_reason = 'Existing message approved during migration',
    moderated_at = created_at
WHERE moderation_status IS NULL;

ALTER TABLE chat_messages
    ALTER COLUMN moderation_status SET NOT NULL,
    ADD CONSTRAINT chat_messages_moderation_status_check
        CHECK (moderation_status IN ('PENDING', 'APPROVED', 'FLAGGED', 'BLOCKED'));

CREATE TABLE chat_message_reports (
    id UUID PRIMARY KEY,
    chat_message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    reported_by_user_id UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'RESOLVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    resolved_by_user_id UUID REFERENCES users(id)
);

CREATE TABLE chat_moderation_audits (
    id UUID PRIMARY KEY,
    chat_message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    action VARCHAR(30) NOT NULL CHECK (action IN (
        'AUTO_APPROVED', 'AUTO_FLAGGED', 'AUTO_BLOCKED', 'USER_REPORTED',
        'MANUAL_APPROVED', 'MANUAL_BLOCKED', 'USER_SANCTIONED'
    )),
    previous_status VARCHAR(20) CHECK (previous_status IN (
        'PENDING', 'APPROVED', 'FLAGGED', 'BLOCKED'
    )),
    new_status VARCHAR(20) NOT NULL CHECK (new_status IN (
        'PENDING', 'APPROVED', 'FLAGGED', 'BLOCKED'
    )),
    reason VARCHAR(1000),
    actor_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_chat_message_open_reporter
    ON chat_message_reports(chat_message_id, reported_by_user_id)
    WHERE status = 'OPEN';
CREATE INDEX idx_chat_messages_moderation
    ON chat_messages(moderation_status, created_at DESC);
CREATE INDEX idx_chat_moderation_audits_message
    ON chat_moderation_audits(chat_message_id, created_at DESC);

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (type IN (
    'NEW_REQUEST', 'NEW_QUOTE', 'QUOTE_ACCEPTED', 'QUOTE_REJECTED',
    'REQUEST_ACCEPTED', 'TECHNICIAN_ON_THE_WAY', 'TECHNICIAN_ARRIVED',
    'SERVICE_STARTED', 'SERVICE_COMPLETED', 'NEW_CHAT_MESSAGE', 'NEW_RATING',
    'PAYMENT_PROOF_UPLOADED', 'SERVICE_EVIDENCE_UPLOADED',
    'PAYMENT_PROOF_VERIFIED', 'CONTENT_MODERATION_ALERT', 'CHAT_MODERATION_ALERT',
    'SERVICE_STATUS_CHANGED', 'LEGAL_ACCEPTANCE_REQUIRED'
));
