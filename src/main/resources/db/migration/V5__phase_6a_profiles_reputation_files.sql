DO $$
BEGIN
IF to_regclass('public.users') IS NULL OR to_regclass('public.ratings') IS NULL THEN
    RETURN;
END IF;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS document_photo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS certificate_photo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS work_experience_description VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS average_rating NUMERIC(3, 2) NOT NULL DEFAULT 5.00,
    ADD COLUMN IF NOT EXISTS completed_services_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS paid_services_count BIGINT NOT NULL DEFAULT 0;

UPDATE users user_row
SET completed_services_count = (
        SELECT COUNT(*) FROM service_requests request
        WHERE request.status IN ('COMPLETED', 'PAID')
          AND (request.client_id = user_row.id OR request.technician_id = user_row.id)
    ),
    paid_services_count = (
        SELECT COUNT(*) FROM payments payment
        WHERE payment.client_id = user_row.id OR payment.technician_id = user_row.id
    );

ALTER TABLE ratings ADD COLUMN IF NOT EXISTS rater_id UUID;
ALTER TABLE ratings ADD COLUMN IF NOT EXISTS rated_user_id UUID;

UPDATE ratings
SET rater_id = client_id,
    rated_user_id = technician_id
WHERE rater_id IS NULL;

ALTER TABLE ratings DROP CONSTRAINT IF EXISTS ratings_service_request_id_key;
ALTER TABLE ratings DROP CONSTRAINT IF EXISTS ratings_client_id_fkey;
ALTER TABLE ratings DROP CONSTRAINT IF EXISTS ratings_technician_id_fkey;
ALTER TABLE ratings ALTER COLUMN rater_id SET NOT NULL;
ALTER TABLE ratings ALTER COLUMN rated_user_id SET NOT NULL;
ALTER TABLE ratings
    ADD CONSTRAINT fk_ratings_rater FOREIGN KEY (rater_id) REFERENCES users(id),
    ADD CONSTRAINT fk_ratings_rated_user FOREIGN KEY (rated_user_id) REFERENCES users(id),
    ADD CONSTRAINT uk_ratings_request_rater UNIQUE (service_request_id, rater_id);
ALTER TABLE ratings DROP COLUMN IF EXISTS client_id;
ALTER TABLE ratings DROP COLUMN IF EXISTS technician_id;

UPDATE users user_row
SET average_rating = COALESCE((
    SELECT ROUND(AVG(rating.score)::numeric, 2)
    FROM ratings rating
    WHERE rating.rated_user_id = user_row.id
), 5.00);

CREATE INDEX IF NOT EXISTS idx_ratings_rated_user_created
    ON ratings(rated_user_id, created_at DESC);

END;
$$;
