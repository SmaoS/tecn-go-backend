ALTER TABLE users
    ADD COLUMN IF NOT EXISTS onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS onboarding_step VARCHAR(40) NOT NULL DEFAULT 'MAIN_DATA',
    ADD COLUMN IF NOT EXISTS profile_selfie_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS document_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS document_front_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS document_back_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS document_single_url VARCHAR(500);

UPDATE users
SET onboarding_completed = TRUE,
    onboarding_step = 'COMPLETED'
WHERE role IN ('ADMIN', 'VERIFIER');

UPDATE users
SET document_front_url = COALESCE(document_front_url, document_photo_url),
    document_type = COALESCE(document_type, 'CC')
WHERE document_photo_url IS NOT NULL
  AND document_front_url IS NULL
  AND document_single_url IS NULL;

ALTER TABLE service_requests
    ADD COLUMN IF NOT EXISTS requested_payment_method VARCHAR(40) NOT NULL DEFAULT 'CASH';

ALTER TABLE service_requests DROP CONSTRAINT IF EXISTS service_requests_status_check;
ALTER TABLE service_requests
    ADD CONSTRAINT service_requests_status_check
    CHECK (status IN (
        'QUOTE_PENDING',
        'QUOTED',
        'QUOTE_ACCEPTED',
        'ON_THE_WAY',
        'ARRIVED',
        'IN_PROGRESS',
        'COMPLETED',
        'PAID',
        'PAYMENT_DISPUTE',
        'CANCELLED'
    ));
