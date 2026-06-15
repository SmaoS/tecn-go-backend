CREATE TABLE content_assets (
    id UUID PRIMARY KEY,
    uploaded_by_user_id UUID NOT NULL REFERENCES users(id),
    kind VARCHAR(40) NOT NULL CHECK (kind IN (
        'PROFILE', 'DOCUMENT', 'CERTIFICATE', 'SERVICE_REQUEST_IMAGE',
        'SERVICE_EVIDENCE', 'PAYMENT_PROOF'
    )),
    file_url VARCHAR(1000) NOT NULL UNIQUE,
    secure_url VARCHAR(1000) NOT NULL,
    public_id VARCHAR(500) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    moderation_status VARCHAR(30) NOT NULL CHECK (moderation_status IN (
        'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'FLAGGED'
    )),
    moderation_reason VARCHAR(1000),
    moderated_at TIMESTAMPTZ,
    moderated_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE content_reports (
    id UUID PRIMARY KEY,
    content_asset_id UUID NOT NULL REFERENCES content_assets(id),
    reported_by_user_id UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'RESOLVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE service_request_images ADD COLUMN content_asset_id UUID REFERENCES content_assets(id);
ALTER TABLE service_evidences ADD COLUMN content_asset_id UUID REFERENCES content_assets(id);
ALTER TABLE payment_proofs ADD COLUMN content_asset_id UUID REFERENCES content_assets(id);

INSERT INTO content_assets (id, uploaded_by_user_id, kind, file_url, secure_url, public_id,
                            content_type, moderation_status, moderation_reason, moderated_at, created_at)
SELECT gen_random_uuid(), sr.client_id, 'SERVICE_REQUEST_IMAGE', sri.image_url, sri.image_url,
       sri.public_id, 'image/jpeg', 'APPROVED', 'Existing content approved during migration',
       NOW(), sri.created_at
FROM service_request_images sri
JOIN service_requests sr ON sr.id = sri.service_request_id
ON CONFLICT (file_url) DO NOTHING;

UPDATE service_request_images sri
SET content_asset_id = ca.id
FROM content_assets ca
WHERE ca.file_url = sri.image_url;

INSERT INTO content_assets (id, uploaded_by_user_id, kind, file_url, secure_url, public_id,
                            content_type, moderation_status, moderation_reason, moderated_at, created_at)
SELECT gen_random_uuid(), se.uploaded_by_user_id, 'SERVICE_EVIDENCE', se.file_url, se.file_url,
       se.public_id,
       CASE WHEN se.file_url LIKE '%.pdf%' THEN 'application/pdf' ELSE 'image/jpeg' END,
       'APPROVED', 'Existing content approved during migration', NOW(), se.created_at
FROM service_evidences se
ON CONFLICT (file_url) DO NOTHING;

UPDATE service_evidences se
SET content_asset_id = ca.id
FROM content_assets ca
WHERE ca.file_url = se.file_url;

INSERT INTO content_assets (id, uploaded_by_user_id, kind, file_url, secure_url, public_id,
                            content_type, moderation_status, moderation_reason, moderated_at, created_at)
SELECT gen_random_uuid(), pp.uploaded_by_user_id, 'PAYMENT_PROOF', pp.file_url, pp.file_url,
       pp.public_id,
       CASE WHEN pp.file_url LIKE '%.pdf%' THEN 'application/pdf' ELSE 'image/jpeg' END,
       'APPROVED', 'Existing content approved during migration', NOW(), pp.created_at
FROM payment_proofs pp
ON CONFLICT (file_url) DO NOTHING;

UPDATE payment_proofs pp
SET content_asset_id = ca.id
FROM content_assets ca
WHERE ca.file_url = pp.file_url;

INSERT INTO content_assets (id, uploaded_by_user_id, kind, file_url, secure_url, public_id,
                            content_type, moderation_status, moderation_reason, moderated_at, created_at)
SELECT gen_random_uuid(), u.id, 'PROFILE', u.profile_photo_url, u.profile_photo_url,
       COALESCE(u.profile_photo_public_id, 'legacy-profile-' || u.id),
       'image/jpeg', 'APPROVED', 'Existing profile approved during migration', NOW(), u.created_at
FROM users u
WHERE u.profile_photo_url IS NOT NULL AND u.profile_photo_url <> ''
ON CONFLICT (file_url) DO NOTHING;

INSERT INTO content_assets (id, uploaded_by_user_id, kind, file_url, secure_url, public_id,
                            content_type, moderation_status, moderation_reason, moderated_at, created_at)
SELECT gen_random_uuid(), u.id, 'DOCUMENT', u.document_photo_url, u.document_photo_url,
       'legacy-document-' || u.id,
       CASE WHEN u.document_photo_url LIKE '%.pdf%' THEN 'application/pdf' ELSE 'image/jpeg' END,
       'APPROVED', 'Existing document approved during migration', NOW(), u.created_at
FROM users u
WHERE u.document_photo_url IS NOT NULL AND u.document_photo_url <> ''
ON CONFLICT (file_url) DO NOTHING;

INSERT INTO content_assets (id, uploaded_by_user_id, kind, file_url, secure_url, public_id,
                            content_type, moderation_status, moderation_reason, moderated_at, created_at)
SELECT gen_random_uuid(), u.id, 'CERTIFICATE', u.certificate_photo_url, u.certificate_photo_url,
       'legacy-certificate-' || u.id,
       CASE WHEN u.certificate_photo_url LIKE '%.pdf%' THEN 'application/pdf' ELSE 'image/jpeg' END,
       'APPROVED', 'Existing certificate approved during migration', NOW(), u.created_at
FROM users u
WHERE u.certificate_photo_url IS NOT NULL AND u.certificate_photo_url <> ''
ON CONFLICT (file_url) DO NOTHING;

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (type IN (
    'NEW_REQUEST', 'NEW_QUOTE', 'QUOTE_ACCEPTED', 'QUOTE_REJECTED',
    'REQUEST_ACCEPTED', 'TECHNICIAN_ON_THE_WAY', 'TECHNICIAN_ARRIVED',
    'SERVICE_STARTED', 'SERVICE_COMPLETED', 'NEW_CHAT_MESSAGE', 'NEW_RATING',
    'PAYMENT_PROOF_UPLOADED', 'SERVICE_EVIDENCE_UPLOADED',
    'PAYMENT_PROOF_VERIFIED', 'CONTENT_MODERATION_ALERT',
    'SERVICE_STATUS_CHANGED', 'LEGAL_ACCEPTANCE_REQUIRED'
));

UPDATE legal_documents
SET active = FALSE
WHERE code IN ('CLIENT_TERMS', 'TECHNICIAN_TERMS') AND active = TRUE;

INSERT INTO legal_documents (id, code, title, version, role_target, content, active, created_at)
VALUES
    (gen_random_uuid(), 'CLIENT_TERMS', 'Términos y condiciones para clientes', '3.0', 'CLIENT',
$legal$El cliente debe utilizar información real, respetar al técnico, verificar su nombre y foto antes de permitir el acceso, permanecer atento durante el servicio, mantener la comunicación dentro de TecnGo y reportar irregularidades.

Está prohibido cargar, enviar o compartir desnudos, contenido sexual, violencia gráfica, documentos falsos, contenido ilegal, material que promueva delitos, acoso, amenazas o cualquier contenido que vulnere derechos de terceros.

TecnGo facilita el contacto entre cliente y técnico, revisa denuncias y evidencias y puede moderar archivos mediante sistemas automáticos y revisión humana. TecnGo puede ocultar o eliminar contenido y suspender usuarios por fraude, abuso, pagos no certificados, falsificación o conductas indebidas.

TecnGo no promueve acuerdos fuera de la plataforma. El usuario es responsable del contenido que carga y autoriza su procesamiento para seguridad, moderación, prevención de fraude y atención de denuncias.$legal$, TRUE, NOW()),

    (gen_random_uuid(), 'TECHNICIAN_TERMS', 'Términos y compromiso del técnico', '3.0', 'TECHNICIAN',
$legal$El técnico debe prestar el servicio de forma honesta, segura y profesional, respetar al cliente, su domicilio y sus bienes, cumplir las cotizaciones acordadas, informar cambios autorizados y cargar evidencias cuando corresponda.

Está prohibido cargar, enviar o compartir desnudos, contenido sexual, violencia gráfica, documentos o certificados falsos, contenido ilegal, material que promueva delitos, acoso, amenazas o cualquier contenido que vulnere derechos de terceros.

TecnGo puede revisar cotizaciones, evidencias, comprobantes, reputación, mensajes y denuncias; aplicar moderación automática o humana; ocultar o eliminar contenido; y suspender o expulsar usuarios por sobrecostos no autorizados, fraude, falsificación, violencia, amenazas o conductas inapropiadas.

Los casos graves pueden ser denunciados por el usuario afectado ante las autoridades competentes.$legal$, TRUE, NOW());

CREATE INDEX idx_content_assets_moderation ON content_assets(moderation_status, created_at);
CREATE INDEX idx_content_reports_status ON content_reports(status, created_at);
