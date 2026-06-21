CREATE TABLE compliance_retention_policies (
    id UUID PRIMARY KEY,
    data_category VARCHAR(60) NOT NULL UNIQUE,
    retention_days INTEGER NOT NULL CHECK (retention_days > 0),
    legal_basis VARCHAR(500) NOT NULL,
    automatic_deletion BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_user_id UUID REFERENCES users(id)
);

CREATE TABLE compliance_data_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    request_type VARCHAR(30) NOT NULL CHECK (request_type IN ('EXPORT', 'ANONYMIZATION')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'REJECTED')),
    reason VARCHAR(1000),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    reviewed_by_user_id UUID REFERENCES users(id)
);

CREATE INDEX idx_compliance_data_requests_status_created
    ON compliance_data_requests(status, requested_at);
CREATE INDEX idx_compliance_data_requests_user_created
    ON compliance_data_requests(user_id, requested_at DESC);

CREATE TABLE compliance_incidents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('OPEN', 'INVESTIGATING', 'CONTAINED', 'RESOLVED')),
    detected_at TIMESTAMPTZ NOT NULL,
    contained_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reported_by_user_id UUID NOT NULL REFERENCES users(id),
    assigned_to_user_id UUID REFERENCES users(id),
    resolution_summary VARCHAR(4000)
);

CREATE INDEX idx_compliance_incidents_status_severity
    ON compliance_incidents(status, severity, detected_at DESC);

CREATE TABLE compliance_access_audits (
    id UUID PRIMARY KEY,
    actor_user_id UUID REFERENCES users(id),
    subject_user_id UUID REFERENCES users(id),
    resource_type VARCHAR(80) NOT NULL,
    resource_id VARCHAR(255),
    action VARCHAR(120) NOT NULL,
    outcome VARCHAR(20) NOT NULL CHECK (outcome IN ('SUCCESS', 'DENIED', 'FAILED')),
    correlation_id VARCHAR(100),
    ip_hash VARCHAR(64),
    details VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_compliance_access_audits_actor_created
    ON compliance_access_audits(actor_user_id, created_at DESC);
CREATE INDEX idx_compliance_access_audits_subject_created
    ON compliance_access_audits(subject_user_id, created_at DESC);
CREATE INDEX idx_compliance_access_audits_resource_created
    ON compliance_access_audits(resource_type, created_at DESC);

INSERT INTO compliance_retention_policies
    (id, data_category, retention_days, legal_basis, automatic_deletion, active)
VALUES
    (gen_random_uuid(), 'ACCESS_AUDIT', 730,
     'Seguridad, prevención de fraude y trazabilidad de accesos privilegiados', TRUE, TRUE),
    (gen_random_uuid(), 'NOTIFICATIONS', 365,
     'Operación del servicio y soporte al usuario', TRUE, TRUE),
    (gen_random_uuid(), 'AUTHENTICATION_METADATA', 180,
     'Seguridad de cuenta y prevención de fraude', TRUE, TRUE),
    (gen_random_uuid(), 'DATA_SUBJECT_REQUESTS', 1825,
     'Demostración de atención de derechos del titular', FALSE, TRUE),
    (gen_random_uuid(), 'SECURITY_INCIDENTS', 1825,
     'Gestión de incidentes y cumplimiento legal', FALSE, TRUE),
    (gen_random_uuid(), 'FINANCIAL_RECORDS', 3650,
     'Obligaciones contables, tributarias, contractuales y antifraude', FALSE, TRUE),
    (gen_random_uuid(), 'SERVICE_RECORDS', 1825,
     'Ejecución contractual, soporte, disputas y defensa de reclamaciones', FALSE, TRUE);

UPDATE legal_documents
SET content = content || E'\n\nGobierno de datos y derechos del titular\n'
    || E'TecnGo aplica periodos de conservación diferenciados según la finalidad, '
    || E'las obligaciones contractuales, contables, tributarias, de seguridad y de prevención de fraude. '
    || E'El titular puede solicitar una copia de sus datos y la anonimización de su cuenta. '
    || E'La anonimización no elimina registros que deban conservarse por obligación legal o para resolver disputas; '
    || E'en esos casos se restringe su uso a la finalidad que justifica la conservación. '
    || E'Los accesos administrativos a información sensible se registran con fines de seguridad y trazabilidad. '
    || E'TecnGo mantiene un procedimiento de detección, contención, investigación y cierre de incidentes de seguridad.',
    version = '4.0'
WHERE code IN ('PRIVACY_POLICY', 'DATA_TREATMENT_POLICY')
  AND active = TRUE
  AND content NOT LIKE '%Gobierno de datos y derechos del titular%';
