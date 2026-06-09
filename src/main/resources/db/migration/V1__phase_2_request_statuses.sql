DO $$
BEGIN
    IF to_regclass('public.service_requests') IS NOT NULL THEN
        ALTER TABLE service_requests DROP CONSTRAINT IF EXISTS service_requests_status_check;
        UPDATE service_requests SET status = 'CREATED' WHERE status = 'OPEN';
        UPDATE service_requests SET status = 'ACCEPTED' WHERE status = 'ASSIGNED';
        ALTER TABLE service_requests
            ADD CONSTRAINT service_requests_status_check
            CHECK (status IN (
                'CREATED', 'PUBLISHED', 'ACCEPTED', 'ON_THE_WAY',
                'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
            ));
    END IF;
END $$;
