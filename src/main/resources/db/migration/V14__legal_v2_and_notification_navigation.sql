ALTER TABLE notifications
    ADD COLUMN route VARCHAR(80),
    ADD COLUMN request_id UUID REFERENCES service_requests(id);

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check CHECK (type IN (
        'NEW_REQUEST', 'NEW_QUOTE', 'QUOTE_ACCEPTED', 'REQUEST_ACCEPTED',
        'TECHNICIAN_ON_THE_WAY', 'TECHNICIAN_ARRIVED', 'SERVICE_STARTED',
        'SERVICE_COMPLETED', 'NEW_CHAT_MESSAGE', 'NEW_RATING',
        'SERVICE_STATUS_CHANGED', 'LEGAL_ACCEPTANCE_REQUIRED'
    ));

UPDATE legal_documents SET active = FALSE WHERE active = TRUE;

INSERT INTO legal_documents (id, code, title, version, role_target, content, active, created_at) VALUES
    (gen_random_uuid(), 'CLIENT_TERMS', 'Términos y condiciones para clientes', '2.0-draft', 'CLIENT',
$legal$BORRADOR PARA REVISIÓN JURÍDICA.

I. Términos y condiciones para clientes

Artículo 1 – Objeto
El presente documento regula la relación entre los usuarios clientes y TecnGo, plataforma digital de intermediación de servicios técnicos especializados.

Artículo 2 – Obligaciones del cliente
• Verificar la identidad del técnico mediante los mecanismos de la plataforma.
• Mantener la comunicación dentro de TecnGo.
• No compartir contraseñas ni información sensible fuera de la aplicación.
• Pagar únicamente a través de los medios autorizados.

Artículo 3 – Responsabilidad de TecnGo
TecnGo responde únicamente por servicios contratados dentro de la plataforma y no por acuerdos externos.

Artículo 4 – Ley aplicable
Se rige por la legislación colombiana en materia de consumo y comercio electrónico (Ley 1480 de 2011).$legal$, TRUE, NOW()),
    (gen_random_uuid(), 'TECHNICIAN_TERMS', 'Términos y compromiso del técnico', '2.0-draft', 'TECHNICIAN',
$legal$BORRADOR PARA REVISIÓN JURÍDICA.

II. Términos y condiciones para técnicos

Artículo 5 – Registro y verificación
El técnico debe aportar identificación válida, certificados y experiencia comprobable.

Artículo 6 – Obligaciones del técnico
• Actuar con honestidad, respeto y seguridad.
• Informar claramente costos y tiempos del servicio.
• Abstenerse de realizar sobrecostos no autorizados.
• Cumplir con normas laborales, tributarias y de seguridad vigentes en Colombia.

Artículo 7 – Sanciones
El incumplimiento puede generar suspensión temporal o expulsión definitiva de la plataforma.

VI. Compromiso y términos del técnico

Artículo 20 – Honestidad y transparencia
Informar claramente costos, tiempos y condiciones del servicio.

Artículo 21 – Seguridad y respeto
Mantener un trato digno y seguro hacia el cliente y su entorno.

Artículo 22 – Prohibiciones
No incurrir en fraude, amenazas, daño intencional o conductas inapropiadas.

Artículo 23 – Sanciones
El incumplimiento puede generar suspensión temporal o expulsión definitiva de la plataforma.$legal$, TRUE, NOW()),
    (gen_random_uuid(), 'DATA_TREATMENT_POLICY', 'Tratamiento de datos personales', '2.0-draft', 'ALL',
$legal$BORRADOR PARA REVISIÓN JURÍDICA.

III. Tratamiento de datos personales

Artículo 8 – Autorización
Clientes y técnicos autorizan expresamente el tratamiento de sus datos conforme a la Ley 1581 de 2012.

Artículo 9 – Finalidad
Los datos se emplean para validar identidad, gestionar servicios, emitir comprobantes y cumplir obligaciones legales.

Artículo 10 – Derechos del titular
Acceder, rectificar, actualizar o suprimir sus datos mediante solicitud a TecnGo.

Artículo 11 – Conservación y transferencia
Los datos se almacenan por el tiempo necesario y, si se transfieren internacionalmente, se hará bajo estándares equivalentes de protección.$legal$, TRUE, NOW()),
    (gen_random_uuid(), 'PRIVACY_POLICY', 'Política de privacidad', '2.0-draft', 'ALL',
$legal$BORRADOR PARA REVISIÓN JURÍDICA.

IV. Política de privacidad

Artículo 12 – Uso limitado
TecnGo solo recolecta la información estrictamente necesaria para operar y prevenir fraude.

Artículo 13 – No divulgación
Los documentos privados no se publican ni se comparten sin autorización.

Artículo 14 – Seguridad de la información
Se aplican medidas técnicas y administrativas para proteger contra acceso no autorizado.

Artículo 15 – Atención de incidentes
Se habilitan canales para reportar vulneraciones de seguridad o privacidad.$legal$, TRUE, NOW()),
    (gen_random_uuid(), 'SAFETY_RECOMMENDATIONS', 'Seguridad y recomendaciones', '2.0-draft', 'ALL',
$legal$BORRADOR PARA REVISIÓN JURÍDICA.

V. Seguridad y recomendaciones

Artículo 16 – Comunicación oficial
Toda interacción debe realizarse dentro de la plataforma.

Artículo 17 – Registro de evidencias
Documentar el servicio con fotos, comprobantes y mensajes en la aplicación.

Artículo 18 – Prevención de fraude
Reportar inmediatamente intentos de suplantación o cobros irregulares.

Artículo 19 – Conducta segura
Evitar compartir datos sensibles y seguir protocolos de seguridad durante el servicio.$legal$, TRUE, NOW());

INSERT INTO notifications (id, user_id, title, message, type, is_read, created_at, route)
SELECT gen_random_uuid(), u.id, 'Documentos legales pendientes',
       'Lee y acepta los términos, políticas y recomendaciones para usar todas las funciones de TecnGo.',
       'LEGAL_ACCEPTANCE_REQUIRED', FALSE, NOW(), 'Legal'
FROM users u
WHERE u.role IN ('CLIENT', 'TECHNICIAN')
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id AND n.type = 'LEGAL_ACCEPTANCE_REQUIRED'
  );
