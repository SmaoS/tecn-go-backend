ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

UPDATE notifications SET type = 'NEW_QUOTE' WHERE type = 'QUOTE_RECEIVED';
UPDATE notifications SET type = 'NEW_CHAT_MESSAGE' WHERE type = 'CHAT_MESSAGE';

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
    CHECK (type IN (
        'NEW_REQUEST',
        'NEW_QUOTE',
        'QUOTE_ACCEPTED',
        'REQUEST_ACCEPTED',
        'TECHNICIAN_ON_THE_WAY',
        'TECHNICIAN_ARRIVED',
        'SERVICE_STARTED',
        'SERVICE_COMPLETED',
        'NEW_CHAT_MESSAGE',
        'NEW_RATING',
        'SERVICE_STATUS_CHANGED'
    ));
