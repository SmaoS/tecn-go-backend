# TecnGo Backend

Monolito modular para el MVP de TecnGo, construido con Java 21, Spring Boot, PostgreSQL,
Spring Security con JWT, JPA/Hibernate y OpenAPI.

## Módulos

`auth`, `users`, `clients`, `technicians`, `services`, `service_requests`, `payments`,
`ratings`, `notifications`, `geolocation`, `admin` y `shared`.

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
| PUT | `/api/v1/service-requests/{id}/confirm-quote` | CLIENT propietario |
| PUT | `/api/v1/service-requests/{id}/status` | CLIENT o técnico asignado |
| GET | `/api/v1/service-requests/{id}/chat` | Participantes |
| POST | `/api/v1/service-requests/{id}/chat/messages` | Participantes |
| PUT | `/api/v1/service-requests/{id}/chat/read` | Participantes |
| GET | `/api/v1/notifications` | JWT |
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
QUOTE_PENDING -> QUOTED -> QUOTE_ACCEPTED -> ON_THE_WAY -> ARRIVED
              -> IN_PROGRESS -> COMPLETED -> PAID
```

Las ocho categorías iniciales son Electricista, Plomero, Técnico de computadores,
Técnico de celulares, Aire acondicionado, Cámaras de seguridad, Internet / redes y
Cerrajería. Los técnicos seleccionan una o más categorías.

Las solicitudes nuevas requieren dirección, latitud y longitud. El endpoint de
disponibles usa Haversine, filtra por categorías del técnico y acepta radios de 1 a
100 km.

El técnico cotiza con `technicianPrice`; la solicitud pasa a `QUOTED` y queda
reservada. El cliente confirma la cotización, se fija `finalPrice` y pasa a
`QUOTE_ACCEPTED`.
Los bloqueos pesimistas evitan cotizaciones simultáneas.

## Pagos y calificaciones

El MVP registra pagos en efectivo cuando el servicio está `COMPLETED`. La comisión se
configura con `PLATFORM_FEE_PERCENTAGE` y por defecto es 10 %. Al confirmar el pago se
guardan el valor total, la comisión y la ganancia neta del técnico, y la solicitud pasa
a `PAID`.

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

## Chat y notificaciones

El chat REST está disponible entre el cliente y el técnico asociado. Los mensajes
guardan fecha de creación y lectura. Los eventos de cotización, confirmación, cambio de
estado y mensaje crean notificaciones internas.

`PushNotificationGateway` desacopla el envío push. En desarrollo se usa una
implementación de logging; para producción debe reemplazarse por Firebase Admin SDK
usando credenciales gestionadas fuera del repositorio.

## Pruebas y build

```bash
mvn test
mvn clean package
docker build -t tecngo-backend .
```

Flyway administra las migraciones versionadas. Hibernate conserva `ddl-auto=update`
durante el MVP para incorporar tablas nuevas; debe cambiarse a `validate` antes de
producción cuando el esquema completo esté cubierto por migraciones.
