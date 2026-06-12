# TecnGo Backend

Monolito modular para el MVP de TecnGo, construido con Java 21, Spring Boot, PostgreSQL,
Spring Security con JWT, JPA/Hibernate y OpenAPI.

## Módulos

`auth`, `users`, `clients`, `technicians`, `services`, `service_requests`, `payments`,
`ratings`, `notifications`, `geolocation`, `technician_location`,
`system_parameters`, `admin` y `shared`.

Autenticación, usuarios, categorías, perfiles técnicos, geolocalización, administración y solicitudes
tienen implementación funcional. El backend se mantiene como monolito modular.

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 16+ o Docker

## Ejecución local

1. Crear la base:

```sql
CREATE USER tecngo WITH PASSWORD 'tecngo';
CREATE DATABASE tecngo OWNER tecngo;
```

2. Copiar `.env.example` a `.env` y exportar sus variables en la terminal.
3. Ejecutar:

```bash
mvn spring-boot:run
```

La API queda en `http://localhost:8080/api` y Swagger en
`http://localhost:8080/api/swagger-ui.html`.

También puede iniciarse desde `tecngo-infra` con Docker Compose.

Para desarrollo local, activar:

```bash
SPRING_PROFILES_ACTIVE=dev
```

Producción usa `application-prod.yml`, Neon con TLS, Cloudinary y `ddl-auto=validate`.
La guía completa está en `tecngo-infra/DEPLOY_RAILWAY.md`.

## Roles y usuario administrador

Los roles disponibles son `CLIENT`, `TECHNICIAN`, `VERIFIER` y `ADMIN`. El JWT incluye
el claim `role` y la respuesta de autenticación incluye `verificationStatus`.
El registro público solo permite `CLIENT` y `TECHNICIAN`; los verificadores se crean
desde el panel administrativo. En desarrollo se crea este administrador si todavía no
existe:

```text
admin@tecngo.local
Admin123!
```

Las credenciales se configuran mediante `ADMIN_EMAIL` y `ADMIN_PASSWORD`.

## Verificación de correo

Resend es el proveedor activo. Los tokens son aleatorios, expiran y en base de datos se
guarda únicamente su hash. En producción `REQUIRE_EMAIL_VERIFICATION=true` bloquea la
creación de solicitudes y las operaciones sensibles del técnico hasta confirmar el
correo. Sin `RESEND_API_KEY`, desarrollo escribe el enlace en los logs.

## Endpoints principales

| Método | Ruta | Acceso |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Público |
| POST | `/api/v1/auth/login` | Público |
| POST | `/api/v1/auth/send-email-verification` | JWT |
| POST | `/api/v1/auth/resend-email-verification` | JWT |
| POST | `/api/v1/auth/verify-email` | Público |
| GET, PUT | `/api/v1/clients/me/profile` | CLIENT propietario |
| GET | `/api/v1/admin/users/pending-documents` | ADMIN o VERIFIER |
| PUT | `/api/v1/admin/users/{id}/verify-documents` | ADMIN o VERIFIER |
| PUT | `/api/v1/admin/users/{id}/reject-documents` | ADMIN o VERIFIER |
| GET | `/api/v1/verifications/pending` | ADMIN o VERIFIER |
| PUT | `/api/v1/verifications/{userId}/verify` | ADMIN o VERIFIER |
| GET, POST | `/api/v1/admin/verifiers` | ADMIN |
| GET | `/api/v1/services` | Público |
| GET | `/api/v1/service-categories` | Público, solo activas |
| GET, POST | `/api/v1/admin/service-categories` | ADMIN |
| PUT, DELETE | `/api/v1/admin/service-categories/{id}` | ADMIN |
| POST | `/api/v1/technicians/profile` | TECHNICIAN |
| GET, PUT | `/api/v1/technicians/me` | TECHNICIAN |
| GET | `/api/v1/admin/technicians/pending` | ADMIN |
| PUT | `/api/v1/admin/technicians/{id}/approve` | ADMIN |
| PUT | `/api/v1/admin/technicians/{id}/reject` | ADMIN |
| POST | `/api/v1/service-requests` | CLIENT |
| GET | `/api/v1/service-requests/mine` | JWT |
| PUT | `/api/v1/service-requests/{id}/publish` | CLIENT propietario |
| GET | `/api/v1/service-requests/available` | TECHNICIAN aprobado |
| GET | `/api/v1/service-requests/available?radiusKm=10` | TECHNICIAN aprobado |
| PUT | `/api/v1/service-requests/{id}/quote` | TECHNICIAN aprobado |
| GET | `/api/v1/service-requests/{id}/quotes` | CLIENT propietario |
| PUT | `/api/v1/service-requests/{id}/confirm-quote` | CLIENT propietario |
| PUT | `/api/v1/service-requests/{id}/quotes/{quoteId}/reject` | CLIENT propietario |
| POST, GET | `/api/v1/service-requests/{id}/images` | Participantes según estado |
| DELETE | `/api/v1/service-requests/{id}/images/{imageId}` | CLIENT antes de aceptación |
| GET, PUT | `/api/v1/technicians/me/location` | TECHNICIAN aprobado |
| GET | `/api/v1/service-requests/{id}/technician-location` | CLIENT con servicio activo |
| GET | `/api/v1/admin/technicians/locations` | ADMIN |
| GET, PUT | `/api/v1/admin/system-parameters` | ADMIN |
| PUT | `/api/v1/service-requests/{id}/status` | CLIENT o técnico asignado |
| GET | `/api/v1/service-requests/{id}/chat` | Participantes |
| POST | `/api/v1/service-requests/{id}/chat/messages` | Participantes |
| PUT | `/api/v1/service-requests/{id}/chat/read` | Participantes |
| GET | `/api/v1/notifications` | JWT |
| GET | `/api/v1/notifications/unread-count` | JWT |
| PUT | `/api/v1/notifications/{id}/read` | Propietario |
| PUT | `/api/v1/users/me/fcm-token` | JWT |
| POST | `/api/v1/files/upload` | JWT, JPG/PNG/PDF |
| GET | `/api/v1/files/{fileName}` | Perfil público; evidencias dueño/ADMIN/VERIFIER |
| GET, PUT | `/api/v1/users/me/profile` | Dueño autenticado |
| POST | `/api/v1/service-requests/{id}/payment/cash` | CLIENT propietario |
| GET | `/api/v1/payments/mine` | CLIENT |
| GET | `/api/v1/technicians/me/earnings` | TECHNICIAN |
| GET | `/api/v1/admin/payments` | ADMIN |
| POST | `/api/v1/service-requests/{id}/ratings` | CLIENT propietario |
| GET | `/api/v1/technicians/{id}/ratings` | Público |
| GET | `/api/v1/technicians/{id}/summary` | Público |
| GET | `/api/actuator/health` | Público |

Registro de cliente:

```json
{
  "fullName": "Ana Torres",
  "email": "ana@example.com",
  "password": "password123",
  "role": "CLIENT"
}
```

El registro inicial solo solicita nombre, correo, contraseña y tipo de cuenta. El
usuario nace en `CREATED`. Al guardar una foto de documento desde su perfil pasa a
`PENDING_VERIFICATION`; un `ADMIN` o `VERIFIER` revisa la evidencia y lo cambia a
`VERIFIED`. Cambiar o retirar el documento invalida la verificación anterior.

La aprobación profesional del técnico es independiente: primero debe tener identidad
`VERIFIED` y después un administrador puede aprobar su perfil técnico.

Flujo de solicitudes:

```text
QUOTE_PENDING -> QUOTE_ACCEPTED -> ON_THE_WAY -> ARRIVED
              -> IN_PROGRESS -> COMPLETED -> PAID
```

Las ocho categorías iniciales son Electricista, Plomero, Técnico de computadores,
Técnico de celulares, Aire acondicionado, Cámaras de seguridad, Internet / redes y
Cerrajería. Los técnicos seleccionan una o más categorías.

Las solicitudes nuevas requieren dirección, latitud y longitud. El endpoint de
disponibles usa Haversine, filtra por categorías del técnico y acepta radios de 1 a
100 km.

Cada técnico aprobado puede tener una sola cotización pendiente por solicitud. Una
cotización expira según `QUOTE_EXPIRATION_MINUTES`; después de rechazo o expiración el
técnico puede crear otra. Un proceso idempotente marca vencimientos cada minuto.

El cliente consulta todas las ofertas con
`GET /api/v1/service-requests/{id}/quotes` y selecciona una enviando:

```json
{
  "quoteId": "uuid-de-la-cotizacion"
}
```

a `PUT /api/v1/service-requests/{id}/confirm-quote`. La oferta seleccionada pasa a
`ACCEPTED`, las demás a `REJECTED`, se asigna el técnico y la solicitud pasa a
`QUOTE_ACCEPTED`. El bloqueo pesimista de la solicitud impide que se acepten dos
cotizaciones concurrentemente.

## Pagos y calificaciones

El MVP registra pagos en efectivo cuando el servicio está `COMPLETED`. La comisión se
lee de `PLATFORM_COMMISSION_PERCENTAGE` y el porcentaje aplicado se guarda en cada pago
para preservar el histórico.

Solo el cliente propietario puede calificar una solicitud pagada y puede hacerlo una
sola vez. El técnico también puede calificar al cliente una vez después del pago. El
historial del cliente, las ganancias del técnico y el consolidado de
comisiones del administrador se calculan desde los pagos persistidos.

## Archivos, perfiles y reputación

Los archivos se validan como JPG, PNG o PDF y se guardan mediante la abstracción
`FileStorage`. Cloudinary es el único proveedor de ejecución en desarrollo y
producción, configurado con `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` y
`CLOUDINARY_API_SECRET`. `FILE_MAX_SIZE_BYTES` controla el tamaño máximo. Las fotos de
perfil son públicas. Documentos y certificados usan assets autenticados y solo son
visibles para su dueño, un administrador o un verificador.

Clientes y técnicos comienzan con reputación 5.00. El promedio se actualiza al recibir
calificaciones y los contadores se incrementan al completar y pagar servicios.

## Polling, chat y notificaciones

El chat REST está disponible entre el cliente y el técnico asociado. Los mensajes
guardan fecha de creación y lectura. Los clientes consultan sus solicitudes en
`/service-requests/my`, los técnicos sus asignadas en `/service-requests/my-assigned`
y las disponibles en `/service-requests/available`.

Los eventos `NEW_REQUEST`, `NEW_QUOTE`, `QUOTE_ACCEPTED`,
`TECHNICIAN_ON_THE_WAY`, `TECHNICIAN_ARRIVED`, `SERVICE_STARTED`,
`SERVICE_COMPLETED`, `NEW_CHAT_MESSAGE` y `NEW_RATING` crean una notificación
persistida y, si existe token, un push FCM.

También se notifican `QUOTE_REJECTED`, `PAYMENT_PROOF_UPLOADED`,
`SERVICE_EVIDENCE_UPLOADED` y `PAYMENT_PROOF_VERIFIED`. Cada push relacionado con
un servicio incluye `requestId` y `route`; la aplicación vuelve a consultar la API
al abrirlo, por lo que el push funciona como aviso y el polling conserva la fuente
de verdad.

`UserPushNotificationService` y `PushNotificationGateway` desacoplan la lógica de
negocio del transporte. Con `FIREBASE_ENABLED=false` se usa logging; con
`FIREBASE_ENABLED=true`, Firebase Admin SDK envía al token nativo registrado. La clave
privada puede configurarse mediante todos sus campos individuales. Para Railway se
recomienda enviar el archivo completo codificado en Base64 mediante
`FIREBASE_CREDENTIALS_BASE64`, evitando problemas con comillas y saltos de línea.

La migración `V11` actualiza el constraint PostgreSQL de `notifications.type` para
aceptar todos los tipos anteriores. Debe desplegarse antes de generar nuevas
notificaciones con `NEW_QUOTE`, `NEW_REQUEST` o los estados de seguimiento.

La migración `V12` incorpora domicilio, GPS único por técnico, parámetros
administrables, expiración de cotizaciones, porcentaje histórico de comisión e imágenes
opcionales. Cloudinary usa `tecngo/profiles`, `tecngo/documents`,
`tecngo/certificates` y `tecngo/service-requests`.

## Evidencias, pagos, denuncias y documentos legales

La migración `V13` agrega evidencias por servicio, comprobantes de pago con revisión,
denuncias, inactivación auditada y documentos legales versionados. Los archivos usan
Cloudinary en `tecngo/service-evidences`, `tecngo/payment-proofs` y
`tecngo/profile-photos`.

Cliente y técnico sólo acceden a archivos de servicios donde participan. `ADMIN` y
`VERIFIER` revisan evidencias y comprobantes; únicamente `ADMIN` inactiva o reactiva
usuarios. Una cuenta inactiva conserva login y consulta de perfil, pero no puede crear,
cotizar, aceptar, avanzar ni pagar servicios.

La captura de perfil no hace reconocimiento biométrico ni comparación documental: queda
`profilePhotoFaceValidated=false` hasta revisión manual mediante
`PUT /v1/verifications/{userId}/profile-photo/verify`.

La migración `V14` publica los documentos legales, crea la notificación legal
inicial y persiste `route`/`requestId` para navegación contextual desde web, mobile y
push FCM.

## Referidos y versiones de app

La migración `V16` crea códigos únicos para técnicos, registros de referidos,
beneficios de servicio sin comisión y configuración de versiones Android/iOS.
El registro acepta `referralCode` opcional. Un referido genera un único beneficio
cuando participa en un servicio pagado con calificación de 5 estrellas.

Si `PLATFORM_COMMISSION_PERCENTAGE=0`, los beneficios permanecen disponibles. Cuando
la comisión sea mayor que cero, el pago consume el beneficio más antiguo y registra
`commissionWaived`, `commissionWaivedReason` y `referralRewardId`.

Endpoints principales:

```text
GET /v1/referrals/validate/{code}
GET /v1/technicians/me/referral-code
GET /v1/technicians/me/referrals
GET /v1/technicians/me/referral-rewards
GET /v1/admin/referrals
GET /v1/app-version/check?platform=ANDROID&currentVersion=1.0.0
GET/PUT /v1/admin/app-versions
```

Las versiones usan formato `x.y.z`; la comparación es numérica por segmento.

## Pruebas y build

```bash
mvn test
mvn clean package
docker build -t tecngo-backend .
```

Flyway administra las migraciones versionadas. Hibernate conserva `ddl-auto=update`
durante el MVP para incorporar tablas nuevas; debe cambiarse a `validate` antes de
producción cuando el esquema completo esté cubierto por migraciones.
