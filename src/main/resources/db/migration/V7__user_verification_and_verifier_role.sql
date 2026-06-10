ALTER TABLE users
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(255),
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS verified_by_user_id UUID;

UPDATE users
SET verification_status = CASE
    WHEN role = 'ADMIN' OR document_photo_url IS NOT NULL THEN 'VERIFIED'
    ELSE 'CREATED'
END
WHERE verification_status IS NULL;

ALTER TABLE users
    ALTER COLUMN verification_status SET DEFAULT 'CREATED',
    ALTER COLUMN verification_status SET NOT NULL;

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'users'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%role%'
    LOOP
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END;
$$;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
        CHECK (role IN ('CLIENT', 'TECHNICIAN', 'VERIFIER', 'ADMIN')),
    ADD CONSTRAINT users_verification_status_check
        CHECK (verification_status IN ('CREATED', 'PENDING_VERIFICATION', 'VERIFIED')),
    ADD CONSTRAINT fk_users_verified_by
        FOREIGN KEY (verified_by_user_id) REFERENCES users(id);

CREATE INDEX IF NOT EXISTS idx_users_verification_status_created
    ON users(verification_status, created_at);
