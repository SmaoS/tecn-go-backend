CREATE INDEX IF NOT EXISTS idx_service_requests_client_created
    ON service_requests (client_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_service_requests_technician_created
    ON service_requests (technician_id, created_at DESC)
    WHERE technician_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_service_requests_city_status_created
    ON service_requests (city_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created_id
    ON notifications (user_id, created_at DESC, id);

CREATE INDEX IF NOT EXISTS idx_chat_messages_room_created_id
    ON chat_messages (chat_room_id, created_at, id);

CREATE INDEX IF NOT EXISTS idx_service_request_images_request_created
    ON service_request_images (service_request_id, created_at);
