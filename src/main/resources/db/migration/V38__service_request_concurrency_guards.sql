CREATE UNIQUE INDEX IF NOT EXISTS uk_service_quotes_accepted_request
    ON service_quotes(service_request_id)
    WHERE status = 'ACCEPTED';
