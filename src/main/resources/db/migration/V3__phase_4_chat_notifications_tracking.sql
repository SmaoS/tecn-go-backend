DO $$
BEGIN
IF to_regclass('public.users') IS NULL THEN
    RETURN;
END IF;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(500);

CREATE TABLE IF NOT EXISTS chat_rooms (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL UNIQUE REFERENCES service_requests(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY,
    chat_room_id UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id),
    message VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_room_created
    ON chat_messages(chat_room_id, created_at);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON notifications(user_id, created_at DESC);

ALTER TABLE service_requests DROP CONSTRAINT IF EXISTS service_requests_status_check;

UPDATE service_requests SET status = 'QUOTED'
WHERE status = 'PUBLISHED'
  AND technician_id IS NOT NULL
  AND technician_price IS NOT NULL;
UPDATE service_requests SET status = 'QUOTE_PENDING'
WHERE status IN ('CREATED', 'PUBLISHED');
UPDATE service_requests SET status = 'QUOTE_ACCEPTED'
WHERE status = 'ACCEPTED';

ALTER TABLE service_requests
    ADD CONSTRAINT service_requests_status_check
    CHECK (status IN (
        'QUOTE_PENDING', 'QUOTED', 'QUOTE_ACCEPTED', 'ON_THE_WAY',
        'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
    ));

END;
$$;
