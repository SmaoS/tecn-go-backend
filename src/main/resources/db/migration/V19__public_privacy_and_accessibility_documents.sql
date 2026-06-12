UPDATE legal_documents
SET active = FALSE
WHERE code IN ('PRIVACY_POLICY', 'DATA_TREATMENT_POLICY', 'ACCESSIBILITY_POLICY')
  AND active = TRUE;

INSERT INTO legal_documents
    (id, code, title, version, role_target, content, active, created_at)
VALUES
    (gen_random_uuid(), 'PRIVACY_POLICY', 'Política de privacidad', '3.0', 'ALL',
$legal$Política de privacidad de TecnGo

Última actualización: 12 de junio de 2026.

1. Responsable
TecnGo es una plataforma digital de intermediación de servicios técnicos. Para consultas sobre privacidad puede escribir a soporte@tecn-go.com.

2. Información que recopilamos
Según las funciones utilizadas, TecnGo puede recopilar nombre, correo electrónico, teléfono, dirección, ubicación aproximada o GPS, foto de perfil, documento de identidad, certificados de técnicos, descripción de experiencia, fotografías y evidencias del servicio, comprobantes de pago, cotizaciones, mensajes, calificaciones, denuncias e información técnica básica del dispositivo.

3. Datos sensibles y documentos privados
El documento de identidad, los certificados y demás archivos privados se usan para procesos de verificación, seguridad y prevención de fraude. No se muestran públicamente. La foto facial puede utilizarse para validación visual cuando esta función esté habilitada. TecnGo no realiza reconocimiento biométrico automatizado en su versión actual.

4. Finalidades
La información se utiliza para registrar y verificar usuarios, operar el marketplace, conectar clientes y técnicos, gestionar solicitudes, cotizaciones, evidencias y pagos, mantener comunicaciones, atender soporte y denuncias, prevenir fraude, proteger a los usuarios y cumplir obligaciones legales.

5. Ubicación
La ubicación permite mostrar solicitudes y técnicos cercanos y apoyar el seguimiento del servicio. El usuario controla los permisos desde su dispositivo. TecnGo procura mostrar ubicaciones aproximadas antes de que exista una asignación.

6. Proveedores tecnológicos
TecnGo puede procesar información mediante Cloudinary para archivos, Neon PostgreSQL para base de datos, Railway para backend, Vercel para web, Firebase y Google para notificaciones, mapas y servicios Android, Google Play para distribución y proveedores futuros de pagos, correo o SMS. Estos proveedores tratan datos únicamente para prestar sus servicios tecnológicos.

7. Conservación y seguridad
La información se conserva durante el tiempo necesario para operar, resolver controversias, prevenir fraude y cumplir obligaciones legales. TecnGo aplica controles técnicos y administrativos, aunque ningún sistema conectado a Internet puede garantizar seguridad absoluta.

8. Derechos del titular
El usuario puede conocer, actualizar y rectificar sus datos, solicitar prueba de autorización, presentar consultas o reclamos, solicitar eliminación cuando legalmente proceda y revocar la autorización cuando sea aplicable. Las solicitudes se reciben en soporte@tecn-go.com.

9. Menores de edad
TecnGo no está dirigida a menores de edad. Los usuarios deben contar con capacidad legal para contratar y utilizar la plataforma.

10. Cambios
Las modificaciones relevantes se publicarán como una nueva versión. Cuando corresponda, TecnGo solicitará una nueva aceptación antes de permitir funciones críticas.$legal$, TRUE, NOW()),

    (gen_random_uuid(), 'DATA_TREATMENT_POLICY', 'Política de tratamiento de datos personales', '3.0', 'ALL',
$legal$Política de tratamiento de datos personales

De acuerdo con la Ley 1581 de 2012 y sus normas complementarias, el usuario autoriza de manera previa, expresa e informada a TecnGo para recolectar, almacenar, consultar, actualizar, usar, circular de forma restringida y suprimir sus datos personales para operar la plataforma, verificar identidades, gestionar servicios, prevenir fraude, atender denuncias y cumplir obligaciones legales.

Los documentos de identidad, certificados y evidencias privadas están protegidos y no se publican. Cuando se soliciten fotografías faciales u otros datos sensibles, su entrega será informada y se utilizará únicamente para validación visual, seguridad y prevención de suplantaciones. El usuario puede abstenerse de suministrar datos sensibles, aunque determinadas funciones de confianza o contratación podrían no estar disponibles.

La aceptación de esta política es obligatoria antes de usar funciones críticas como solicitar o cotizar servicios, aceptar asignaciones, pagar o cargar evidencias. El titular puede ejercer sus derechos de consulta, actualización, rectificación, supresión o revocación cuando legalmente proceda escribiendo a soporte@tecn-go.com.

Las transferencias o transmisiones internacionales a proveedores tecnológicos se realizan para alojar y operar la plataforma bajo medidas contractuales y técnicas razonables de protección.$legal$, TRUE, NOW()),

    (gen_random_uuid(), 'ACCESSIBILITY_POLICY', 'Política de accesibilidad', '1.0', 'ALL',
$legal$Compromiso de accesibilidad de TecnGo

TecnGo trabaja para que su plataforma sea comprensible y utilizable por la mayor cantidad posible de personas.

Nuestro compromiso incluye mejorar progresivamente la legibilidad, utilizar textos claros, mantener contraste adecuado, identificar botones y controles, facilitar la navegación por teclado en la web y aumentar la compatibilidad con lectores de pantalla y tecnologías de asistencia.

Las aplicaciones se diseñan para adaptarse a diferentes tamaños y orientaciones de pantalla. Continuaremos revisando etiquetas, mensajes de error, áreas táctiles y descripciones de imágenes.

Si encuentra una barrera de accesibilidad, puede reportarla a soporte@tecn-go.com indicando la pantalla, dispositivo y dificultad encontrada. Revisaremos el caso para priorizar una solución razonable.$legal$, TRUE, NOW());
