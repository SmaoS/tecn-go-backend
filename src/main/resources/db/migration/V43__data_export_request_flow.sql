ALTER TABLE compliance_data_requests
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS export_file_url VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS sent_at TIMESTAMPTZ;

DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT conname INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'compliance_data_requests'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) LIKE '%status%';

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE compliance_data_requests DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE compliance_data_requests
    ADD CONSTRAINT compliance_data_requests_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'SENT', 'COMPLETED', 'REJECTED'));

CREATE INDEX IF NOT EXISTS idx_compliance_export_requests_status
    ON compliance_data_requests(request_type, status, requested_at)
    WHERE request_type = 'EXPORT';
