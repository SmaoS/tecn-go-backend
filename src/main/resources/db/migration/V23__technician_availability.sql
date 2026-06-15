ALTER TABLE technician_profiles
    ADD COLUMN IF NOT EXISTS available BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_technician_profiles_available_status
    ON technician_profiles(available, status);
