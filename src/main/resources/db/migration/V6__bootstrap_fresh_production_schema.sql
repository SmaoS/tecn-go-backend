DO $$
BEGIN
IF to_regclass('public.users') IS NOT NULL THEN
    RETURN;
END IF;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL CHECK (role IN ('CLIENT', 'TECHNICIAN', 'ADMIN')),
    created_at TIMESTAMPTZ NOT NULL,
    fcm_token VARCHAR(500),
    profile_photo_url VARCHAR(500),
    document_photo_url VARCHAR(500),
    certificate_photo_url VARCHAR(500),
    work_experience_description VARCHAR(1000),
    average_rating NUMERIC(3, 2) NOT NULL DEFAULT 5.00,
    completed_services_count BIGINT NOT NULL DEFAULT 0,
    paid_services_count BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE service_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE technician_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    document_number VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    status VARCHAR(255) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'BLOCKED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE technician_profile_categories (
    technician_profile_id UUID NOT NULL REFERENCES technician_profiles(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES service_categories(id),
    PRIMARY KEY (technician_profile_id, category_id)
);

CREATE TABLE service_requests (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES users(id),
    technician_id UUID REFERENCES users(id),
    category_id UUID NOT NULL REFERENCES service_categories(id),
    description VARCHAR(1000) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    estimated_price NUMERIC(12, 2),
    technician_price NUMERIC(12, 2),
    final_price NUMERIC(12, 2),
    status VARCHAR(255) NOT NULL CHECK (status IN (
        'QUOTE_PENDING', 'QUOTED', 'QUOTE_ACCEPTED', 'ON_THE_WAY',
        'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'PAID', 'CANCELLED'
    )),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE chat_rooms (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL UNIQUE REFERENCES service_requests(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    chat_room_id UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id),
    message VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ
);
CREATE INDEX idx_chat_messages_room_created ON chat_messages(chat_room_id, created_at);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(255) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL UNIQUE REFERENCES service_requests(id),
    client_id UUID NOT NULL REFERENCES users(id),
    technician_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(12, 2) NOT NULL,
    platform_fee NUMERIC(12, 2) NOT NULL,
    technician_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    method VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_payments_client_created ON payments(client_id, created_at DESC);
CREATE INDEX idx_payments_technician_created ON payments(technician_id, created_at DESC);

CREATE TABLE ratings (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL REFERENCES service_requests(id),
    rater_id UUID NOT NULL REFERENCES users(id),
    rated_user_id UUID NOT NULL REFERENCES users(id),
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_ratings_request_rater UNIQUE (service_request_id, rater_id)
);
CREATE INDEX idx_ratings_rated_user_created ON ratings(rated_user_id, created_at DESC);

END;
$$;
