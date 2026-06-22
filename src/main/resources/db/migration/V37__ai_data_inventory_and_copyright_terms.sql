UPDATE legal_documents
SET active = FALSE
WHERE code IN (
    'PRIVACY_POLICY',
    'DATA_TREATMENT_POLICY',
    'CLIENT_TERMS',
    'TECHNICIAN_TERMS',
    'PRIVACY_LABEL'
) AND active = TRUE;

INSERT INTO legal_documents
    (id, code, title, version, role_target, content, active, created_at)
VALUES
    (gen_random_uuid(), 'PRIVACY_POLICY', 'Política de privacidad', '5.0', 'ALL',
$legal$Política de privacidad de TecnGo

Última actualización: 22 de junio de 2026.

1. Responsable
TecnGo es una plataforma digital de intermediación de servicios técnicos. Las consultas, reclamos y solicitudes sobre privacidad se reciben en soporte@tecn-go.com.

2. Datos que podemos recolectar
Según las funciones utilizadas, TecnGo puede tratar:
• Identificación y contacto: nombre, correo, teléfono, tipo y número de documento.
• Cuenta y seguridad: contraseña cifrada, roles, estado de verificación, sesiones, dirección IP transformada o registrada para seguridad, agente de usuario, intentos de acceso y registros de auditoría.
• Perfil profesional: especialidades, experiencia, certificados, disponibilidad, reputación y servicios completados.
• Ubicación: país, departamento, ciudad, dirección, barrio y coordenadas aproximadas o precisas cuando el usuario autoriza GPS o selecciona un punto en el mapa.
• Contenido: foto de perfil, selfie, documento de identidad, certificados, fotografías del servicio, evidencias, comprobantes de pago, mensajes, comentarios, denuncias y archivos PDF.
• Operación del marketplace: solicitudes, categorías, cotizaciones, precios, estados, asignaciones, historial, calificaciones y aceptación de documentos legales.
• Información financiera limitada: método de pago, valores, comisiones, saldos, recargas, comprobantes y referencias de transacción. TecnGo no almacena números completos de tarjetas ni códigos de seguridad; estos son tratados por el proveedor de pagos.
• Dispositivo y funcionamiento: token de notificaciones, plataforma, versión de la app, identificadores técnicos necesarios, diagnósticos, errores, rendimiento y eventos de seguridad.

No accedemos a contactos, calendario, salud, archivos ajenos seleccionados por el usuario ni micrófono para grabaciones permanentes. Los permisos del dispositivo solo se usan cuando una función los necesita.

3. Datos sensibles y acceso restringido
Documentos, selfies, certificados, evidencias privadas y ubicación exacta tienen acceso controlado. Solo pueden consultarlos el titular, participantes autorizados del servicio y personal administrador o verificador cuando sea necesario. No se publican como contenido abierto.

4. Inteligencia artificial y decisiones automatizadas
TecnGo no utiliza IA generativa para crear cotizaciones, decidir precios, asignar técnicos, aprobar identidades ni reemplazar decisiones humanas.

Cuando el servicio de moderación automática está contratado y habilitado, una imagen puede analizarse mediante Cloudinary y servicios de moderación basados en aprendizaje automático, como AWS Rekognition, para detectar posibles categorías de desnudez, contenido sexual, violencia o material inseguro. El resultado puede aprobar, rechazar o enviar la imagen a revisión humana. Si el proveedor no está disponible, la imagen queda pendiente de revisión manual.

El chat usa principalmente reglas técnicas y patrones predefinidos para detectar amenazas, datos sensibles, fraude, enlaces o lenguaje riesgoso. Este filtro no es IA generativa.

La selfie se valida visualmente por personal autorizado. TecnGo no realiza identificación biométrica automatizada ni reconocimiento facial para identificar personas en su versión actual.

Los sistemas automáticos pueden equivocarse. El usuario puede reportar una decisión y solicitar revisión humana en soporte@tecn-go.com. Una sanción definitiva debe permitir revisión por personal autorizado.

5. Finalidades
Los datos se usan para crear y proteger cuentas, verificar identidad, conectar clientes y técnicos, operar solicitudes y cotizaciones, mostrar cercanía, gestionar comunicaciones, evidencias, pagos, reputación, soporte, denuncias, prevención de fraude, moderación, cumplimiento legal, continuidad y mejora técnica.

6. Proveedores y transmisiones
TecnGo utiliza proveedores que pueden procesar datos por cuenta de la plataforma: Railway, Neon, Cloudinary, Firebase, Google Maps y servicios Android, Vercel, Sentry, Resend, Twilio, Wompi, Google Play y otros sustitutos equivalentes. Se comparte únicamente la información necesaria para prestar cada función, bajo controles contractuales y técnicos razonables.

7. Derechos de autor y moderación de contenido
Cloudinary y los sistemas de moderación de seguridad no verifican quién es propietario de una imagen ni detectan de forma confiable infracciones de derechos de autor. El usuario debe cargar únicamente contenido propio, autorizado, de dominio público o permitido por la ley.

Los titulares de derechos pueden reportar contenido indicando la obra, ubicación del contenido, acreditación de titularidad y datos de contacto. TecnGo podrá ocultarlo preventivamente, solicitar soportes, retirarlo y sancionar cuentas reincidentes. Una controversia de propiedad intelectual puede requerir revisión humana y no se resuelve exclusivamente mediante automatización.

8. Conservación, exportación y eliminación
TecnGo aplica periodos diferenciados de conservación. El usuario puede exportar sus datos y solicitar anonimización desde su perfil. La anonimización no elimina registros financieros, contractuales, antifraude o de disputas que deban conservarse por obligación legal; su uso se restringe a esa finalidad.

9. Derechos del titular
El titular puede conocer, actualizar, rectificar y solicitar supresión; pedir prueba de autorización; presentar consultas o reclamos; solicitar revisión humana de moderación; y revocar autorizaciones cuando legalmente proceda.

10. Seguridad y cambios
TecnGo aplica controles de acceso, cifrado en tránsito, sesiones revocables, auditoría y almacenamiento privado. Ningún sistema garantiza seguridad absoluta. Los cambios materiales se publican como una nueva versión y pueden requerir nueva aceptación.$legal$, TRUE, NOW()),

    (gen_random_uuid(), 'DATA_TREATMENT_POLICY', 'Política de tratamiento de datos personales', '5.0', 'ALL',
$legal$Política de tratamiento de datos personales

De acuerdo con la Ley 1581 de 2012 y sus normas complementarias, el usuario autoriza a TecnGo para recolectar, almacenar, consultar, actualizar, utilizar, transmitir de forma restringida, moderar, conservar y suprimir datos personales necesarios para operar la plataforma.

Categorías tratadas:
• Identificación, contacto y datos de cuenta.
• Ubicación aproximada y precisa autorizada.
• Imágenes, documentos, certificados, evidencias, comprobantes y mensajes.
• Solicitudes, cotizaciones, pagos, saldos, calificaciones, denuncias e historial.
• Datos técnicos, sesiones, notificaciones, diagnósticos y seguridad.

Finalidades:
• Verificación de identidad y prevención de suplantación.
• Prestación, asignación, seguimiento y cierre de servicios.
• Comunicación, soporte, pagos, reputación y atención de controversias.
• Moderación automática o humana, prevención de fraude y seguridad.
• Analítica operativa, diagnóstico, auditoría y cumplimiento legal.

TecnGo puede emplear análisis automatizado de imágenes mediante proveedores de moderación basados en aprendizaje automático cuando estén habilitados. No realiza reconocimiento facial automatizado ni toma decisiones jurídicas o contractuales definitivas exclusivamente mediante IA. El titular puede solicitar revisión humana.

Los datos se transmiten a proveedores tecnológicos y de pago únicamente en la medida necesaria. TecnGo no comercializa datos personales ni vende información para publicidad.

El titular puede ejercer consulta, actualización, rectificación, exportación, supresión, anonimización o revocación cuando proceda escribiendo a soporte@tecn-go.com o usando las opciones disponibles en su perfil.$legal$, TRUE, NOW()),

    (gen_random_uuid(), 'CLIENT_TERMS', 'Términos y condiciones para clientes', '4.0', 'CLIENT',
$legal$Términos y condiciones para clientes

1. El cliente debe registrar información real, proteger su cuenta, verificar al técnico mediante la plataforma, mantener la comunicación en TecnGo, respetar al prestador y pagar por los medios autorizados.

2. Está prohibido publicar desnudos, contenido sexual, violencia gráfica, documentos falsos, fraude, amenazas, acoso, material ilegal, malware, datos personales de terceros sin autorización o contenido que vulnere derechos de terceros.

3. Propiedad intelectual
El cliente declara que las fotografías, textos, documentos y demás archivos que carga son propios, cuentan con autorización suficiente, están en dominio público o su uso está permitido por la ley. Conserva la titularidad de su contenido y concede a TecnGo una licencia limitada, no exclusiva y revocable cuando legalmente proceda para almacenarlo, transformarlo técnicamente, moderarlo y mostrarlo únicamente a usuarios autorizados con el fin de operar el servicio.

Cloudinary no certifica autoría ni derechos de uso. TecnGo puede ocultar o retirar contenido denunciado, solicitar soportes y suspender cuentas reincidentes. Las denuncias de derechos de autor se reciben en soporte@tecn-go.com.

4. Moderación
TecnGo puede aplicar reglas automáticas, servicios de moderación de imágenes basados en aprendizaje automático y revisión humana. Estos mecanismos pueden equivocarse y no sustituyen la responsabilidad del usuario. El cliente puede solicitar revisión humana.

5. TecnGo actúa como plataforma de intermediación. No responde por acuerdos celebrados fuera de la plataforma ni por contenido cargado sin autorización. Puede conservar evidencias para atender disputas, fraude u obligaciones legales.$legal$, TRUE, NOW()),

    (gen_random_uuid(), 'TECHNICIAN_TERMS', 'Términos y compromiso del técnico', '4.0', 'TECHNICIAN',
$legal$Términos y compromiso del técnico

1. El técnico debe aportar información real, identificación y experiencia verificables; actuar con honestidad, seguridad y respeto; informar costos y tiempos; cumplir la cotización aceptada y registrar evidencias cuando corresponda.

2. Está prohibido publicar desnudos, contenido sexual, violencia gráfica, documentos o certificados falsos, fraude, amenazas, acoso, material ilegal, malware, datos de terceros sin autorización o contenido que vulnere derechos de terceros.

3. Propiedad intelectual
El técnico declara que las fotografías, certificados, textos y archivos cargados son propios, autorizados, de dominio público o permitidos por la ley. Conserva su titularidad y concede a TecnGo una licencia limitada y no exclusiva para alojar, transformar técnicamente, moderar y mostrar el contenido a usuarios autorizados para operar y verificar el servicio.

Cloudinary y la moderación automática no certifican derechos de autor. TecnGo puede retirar material denunciado, pedir evidencia de licencia o autoría y suspender cuentas reincidentes. Las denuncias se reciben en soporte@tecn-go.com.

4. Moderación y revisión
TecnGo puede usar filtros por reglas, moderación automática de imágenes basada en aprendizaje automático y revisión humana. No usa IA para fijar el precio del técnico, aceptar cotizaciones en su nombre ni aprobar definitivamente su identidad. Las decisiones sancionatorias relevantes pueden solicitar revisión humana.

5. El incumplimiento puede generar ocultamiento de contenido, suspensión, bloqueo o expulsión, sin perjuicio de denuncias ante autoridades o reclamaciones de titulares de derechos.$legal$, TRUE, NOW()),

    (gen_random_uuid(), 'PRIVACY_LABEL', 'Datos recolectados y etiqueta de privacidad', '1.0', 'ALL',
$legal$Datos recolectados y etiqueta de privacidad

Esta ficha resume las categorías utilizadas en la aplicación y sirve como referencia para la declaración de Seguridad de datos de Google Play. La declaración definitiva debe coincidir con la versión publicada y con la configuración real de proveedores.

Datos personales:
• Nombre, correo, teléfono, dirección, ciudad, barrio, identificador de usuario y documento.
• Finalidad: cuenta, contacto, verificación, seguridad y prestación del servicio.

Ubicación:
• Ubicación aproximada y precisa autorizada.
• Finalidad: servicios cercanos, selección del lugar, rutas y seguimiento.
• La ubicación exacta del servicio se restringe antes de aceptar una cotización.

Información financiera:
• Historial de pagos, método, valores, comisiones, saldo, recargas, comprobantes y referencias.
• TecnGo no almacena el número completo de tarjeta ni CVV.

Mensajes y contenido:
• Chat, comentarios, cotizaciones, calificaciones y denuncias.
• Fotos de perfil, selfies, documentos, certificados, imágenes del servicio, evidencias, comprobantes y PDF.

Actividad de la aplicación:
• Solicitudes, búsquedas por categoría o ciudad, cotizaciones, estados, asignaciones, aceptaciones legales e interacciones necesarias para operar.

Información de la aplicación y rendimiento:
• Versión, plataforma, registros de errores, diagnósticos, rendimiento, correlation ID y eventos técnicos.

Identificadores del dispositivo y seguridad:
• Token de notificaciones, agente de usuario, sesión, dirección IP o su representación protegida e identificadores técnicos necesarios.

No recolectamos para publicidad:
• Contactos, calendario, salud, actividad física ni contenido de otras aplicaciones.
• TecnGo no vende datos personales ni los usa para publicidad comportamental.

Los datos se cifran en tránsito. Los documentos y archivos privados tienen acceso controlado. El usuario puede solicitar exportación y anonimización desde su perfil. Los proveedores tecnológicos procesan datos para prestar almacenamiento, mapas, notificaciones, pagos, correo, SMS, diagnóstico e infraestructura.$legal$, TRUE, NOW());
