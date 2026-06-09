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

## Roles y usuario administrador

Los roles disponibles son `CLIENT`, `TECHNICIAN` y `ADMIN`. El JWT incluye el claim
`role`. En desarrollo se crea este administrador si todavía no existe:

```text
admin@tecngo.local
Admin123!
```

Las credenciales se configuran mediante `ADMIN_EMAIL` y `ADMIN_PASSWORD`.

## Endpoints principales

| Método | Ruta | Acceso |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Público |
| POST | `/api/v1/auth/login` | Público |
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
| POST | `/api/v1/files/upload` | Público, JPG/PNG/PDF |
| GET | `/api/v1/files/{fileName}` | Perfil público; evidencias dueño/ADMIN |
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
`FileStorage`. La implementación inicial usa `FILE_STORAGE_PATH` y un límite
`FILE_MAX_SIZE_BYTES`; puede reemplazarse por una implementación S3 sin cambiar los
controladores. Las fotos de perfil son públicas. Documentos y certificados solo son
visibles para su dueño o un administrador.

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
