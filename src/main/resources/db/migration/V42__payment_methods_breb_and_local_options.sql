ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_method_check;
ALTER TABLE payments
    ADD CONSTRAINT payments_method_check
    CHECK (method IN (
        'CASH',
        'BREB',
        'NEQUI',
        'DAVIPLATA',
        'BANCOLOMBIA',
        'DAVIVIENDA',
        'WOMPI',
        'MERCADO_PAGO',
        'PAYU'
    ));

ALTER TABLE service_requests DROP CONSTRAINT IF EXISTS service_requests_requested_payment_method_check;
ALTER TABLE service_requests
    ADD CONSTRAINT service_requests_requested_payment_method_check
    CHECK (requested_payment_method IN (
        'CASH',
        'BREB',
        'NEQUI',
        'DAVIPLATA',
        'BANCOLOMBIA',
        'DAVIVIENDA',
        'WOMPI',
        'MERCADO_PAGO',
        'PAYU'
    ));
