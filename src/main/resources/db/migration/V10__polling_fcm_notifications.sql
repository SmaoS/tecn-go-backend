ALTER TABLE users
    ADD COLUMN IF NOT EXISTS fcm_token_updated_at TIMESTAMPTZ;

UPDATE notifications SET type = 'NEW_QUOTE' WHERE type = 'QUOTE_RECEIVED';
UPDATE notifications SET type = 'NEW_CHAT_MESSAGE' WHERE type = 'CHAT_MESSAGE';

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications(user_id, is_read, created_at DESC);
