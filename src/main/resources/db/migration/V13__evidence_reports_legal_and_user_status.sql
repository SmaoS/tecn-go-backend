ALTER TABLE users
    ADD COLUMN account_status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN inactive_reason VARCHAR(40),
    ADD COLUMN inactive_comment VARCHAR(1000),
    ADD COLUMN inactivated_at TIMESTAMPTZ,
    ADD COLUMN inactivated_by_user_id UUID REFERENCES users(id),
    ADD COLUMN profile_photo_public_id VARCHAR(500),
    ADD COLUMN profile_photo_face_validated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN profile_photo_verified_by_user_id UUID REFERENCES users(id),
    ADD COLUMN profile_photo_verified_at TIMESTAMPTZ,
    ADD CONSTRAINT users_account_status_check CHECK (account_status IN (
        'ACTIVE', 'INACTIVE_PAYMENT', 'INACTIVE_REPORT', 'INACTIVE_ADMIN', 'BLOCKED', 'DELETED_LOGICAL'
    ));

CREATE TABLE service_evidences (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL REFERENCES service_requests(id),
    uploaded_by_user_id UUID NOT NULL REFERENCES users(id),
    uploaded_by_role VARCHAR(30) NOT NULL CHECK (uploaded_by_role IN ('CLIENT', 'TECHNICIAN')),
    evidence_type VARCHAR(40) NOT NULL CHECK (evidence_type IN (
        'BEFORE_SERVICE', 'DURING_SERVICE', 'AFTER_SERVICE', 'PAYMENT_PROOF',
        'DAMAGE_REPORT', 'OTHER'
    )),
    file_url VARCHAR(1000) NOT NULL,
    public_id VARCHAR(500) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_service_evidences_request ON service_evidences(service_request_id, created_at);

CREATE TABLE payment_proofs (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL REFERENCES service_requests(id),
    uploaded_by_user_id UUID NOT NULL REFERENCES users(id),
    file_url VARCHAR(1000) NOT NULL,
    public_id VARCHAR(500) NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(40) NOT NULL CHECK (payment_method IN (
        'CASH', 'TRANSFER', 'WOMPI', 'MERCADO_PAGO', 'PAYU', 'OTHER'
    )),
    status VARCHAR(40) NOT NULL CHECK (status IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED')),
    reviewed_by_user_id UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    review_comment VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_payment_proofs_request ON payment_proofs(service_request_id, created_at);
CREATE INDEX idx_payment_proofs_status ON payment_proofs(status, created_at);
CREATE UNIQUE INDEX uk_payment_proofs_approved_request ON payment_proofs(service_request_id)
    WHERE status = 'APPROVED';

CREATE TABLE user_reports (
    id UUID PRIMARY KEY,
    service_request_id UUID NOT NULL REFERENCES service_requests(id),
    reporter_user_id UUID NOT NULL REFERENCES users(id),
    reported_user_id UUID NOT NULL REFERENCES users(id),
    reporter_role VARCHAR(30) NOT NULL,
    reported_role VARCHAR(30) NOT NULL,
    reason VARCHAR(50) NOT NULL CHECK (reason IN (
        'PAYMENT_ISSUE', 'INAPPROPRIATE_BEHAVIOR', 'OVERCHARGE', 'NO_SHOW',
        'BAD_SERVICE', 'FRAUD', 'SECURITY_RISK', 'OTHER'
    )),
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'REJECTED')),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    created_at TIMESTAMPTZ NOT NULL,
    reviewed_by_user_id UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    resolution_comment VARCHAR(2000)
);
CREATE INDEX idx_user_reports_status ON user_reports(status, severity, created_at);

CREATE TABLE legal_documents (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    title VARCHAR(255) NOT NULL,
    version VARCHAR(40) NOT NULL,
    role_target VARCHAR(30) NOT NULL CHECK (role_target IN ('CLIENT', 'TECHNICIAN', 'ALL')),
    content TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(code, version)
);
CREATE UNIQUE INDEX uk_legal_documents_active_code_role
    ON legal_documents(code, role_target) WHERE active = TRUE;

CREATE TABLE legal_acceptances (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    legal_document_id UUID NOT NULL REFERENCES legal_documents(id),
    accepted_at TIMESTAMPTZ NOT NULL,
    ip_address VARCHAR(100),
    user_agent VARCHAR(500),
    UNIQUE(user_id, legal_document_id)
);
CREATE INDEX idx_legal_acceptances_user ON legal_acceptances(user_id, accepted_at);

INSERT INTO system_parameters (id, parameter_key, parameter_value, description, type, active, updated_at) VALUES
    (gen_random_uuid(), 'MAX_SERVICE_EVIDENCE_FILES', '10', 'Máximo de evidencias por servicio', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'MAX_PAYMENT_PROOF_FILES', '3', 'Máximo de comprobantes por servicio', 'INTEGER', TRUE, NOW()),
    (gen_random_uuid(), 'REQUIRE_LEGAL_ACCEPTANCE', 'true', 'Exige aceptación legal para acciones críticas', 'BOOLEAN', TRUE, NOW()),
    (gen_random_uuid(), 'REQUIRE_PROFILE_FACE_DETECTION', 'false', 'Exige detección facial automática', 'BOOLEAN', TRUE, NOW())
ON CONFLICT (parameter_key) DO NOTHING;

INSERT INTO legal_documents (id, code, title, version, role_target, content, active, created_at) VALUES
    (gen_random_uuid(), 'CLIENT_TERMS', 'Términos y condiciones para clientes', '1.0-draft', 'CLIENT',
     'BORRADOR PARA REVISIÓN JURÍDICA. Verifica nombre y foto del técnico, permanece atento durante el servicio y no compartas claves, documentos ni información sensible fuera de la plataforma. TecnGo revisará denuncias y evidencias relacionadas con servicios registrados.', TRUE, NOW()),
    (gen_random_uuid(), 'TECHNICIAN_TERMS', 'Compromiso y términos del técnico', '1.0-draft', 'TECHNICIAN',
     'BORRADOR PARA REVISIÓN JURÍDICA. El técnico debe actuar de forma honesta, segura y profesional. Sobrecostos no autorizados, fraude, amenazas, daño intencional o conductas inapropiadas pueden causar suspensión o expulsión.', TRUE, NOW()),
    (gen_random_uuid(), 'PRIVACY_POLICY', 'Política de privacidad', '1.0-draft', 'ALL',
     'BORRADOR PARA REVISIÓN JURÍDICA. TecnGo usa los datos necesarios para operar el servicio, verificar usuarios, prevenir fraude y atender incidentes. Los documentos privados no se publican.', TRUE, NOW()),
    (gen_random_uuid(), 'DATA_TREATMENT_POLICY', 'Tratamiento de datos personales', '1.0-draft', 'ALL',
     'BORRADOR PARA REVISIÓN JURÍDICA. El usuario puede aportar identificación, foto, certificados, evidencias y comprobantes. Puede consultar y actualizar sus datos conforme a la política aplicable.', TRUE, NOW()),
    (gen_random_uuid(), 'SAFETY_RECOMMENDATIONS', 'Seguridad y recomendaciones', '1.0-draft', 'ALL',
     'BORRADOR PARA REVISIÓN JURÍDICA. Mantén comunicación dentro de TecnGo, documenta el servicio y reporta riesgos, fraude o conductas inseguras.', TRUE, NOW());
