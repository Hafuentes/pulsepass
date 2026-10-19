# PulsePass — Plataforma de Eventos, Artistas y Entradas

Caso de estudio académico (persistencia, servicios, controladores REST y seguridad) con Java 21, Spring Boot 4, JPA, PostgreSQL y Flyway.

## Requsitos previos

- **Java 21** o superior.
- **Docker Desktop** (Obligatorio para ejecutar los tests de integración con Tescontainers).
- **Maven** o Maven Wrapper.

## Tecnologias y Arquitecturas

- **Lenguaje:** Java 21
- **Framework:** Spring Boot 4 / Spring Data JPA
- **Base de Datos:** PostgreSQL
- **Migraciones de Esquema:** Flyway
- **Pruebas de Integración:** Tescontainers + JUnit 5 + AssertJ

## Esquema de Base de Datos (Flyway)

Las migraciones se encuentran en `src/main/resources/db/migration`;
- `V1__create__schema.sql`: Creación de las tablas `venues`, `events`, `artists`, `events_artists`, `users`, `user_profiles` y `tickets`.
- `V2__insert_initial_artists.sql`: Inserción del catálogo inicial de artistas.
- `V3__add_streaming_url_to_event.sql`: Evolución de la tabla `events` para soportar URLs de streaming.
- `V4__security_credentials_roles.sql`: Fase 4. Tablas `user_credentials` (hash BCrypt, 1:1 con `users`) y `user_roles` (USER/STAFF/ADMIN). Los usuarios previos quedan **sin credenciales** (no pueden iniciar sesión hasta una activación segura; no se asignan contraseñas por defecto).

## Ejecución de Pruebas de Integración
Para ejecutar todas las pruebas contra una instancia real de PostgreSQL gestionada por Testcontainers:

```bash
./mvnw clean test
```

## Capa de Controladores REST (Fase 3)

La API expone los 20 metodos de los Services mediante HTTP. Los Controllers solo dependen de Services, retornan DTOs dentro de `ResponseEntity` y los errores se centralizan en `GlobalExceptionHandler` (`@RestControllerAdvice`) usando el contrato `ErrorResponse`.

### Endpoints principales

| Metodo | Endpoint | Exito |
|--------|----------|-------|
| GET | `/api/venues/{code}` | 200 |
| GET | `/api/venues/active` | 200 |
| POST | `/api/events` | 201 |
| GET | `/api/events/{eventCode}` | 200 |
| GET | `/api/events/published` | 200 |
| PATCH | `/api/events/{eventCode}/publish` | 200 |
| POST | `/api/events/{eventCode}/artists/{artistId}` | 200 |
| GET | `/api/events/by-artist?stageName=...` | 200 |
| GET | `/api/events/{eventCode}/tickets/paid` | 200 |
| GET | `/api/artists/{id}` | 200 |
| GET | `/api/artists/by-stage-name?stageName=...` | 200 |
| GET | `/api/artists/active` | 200 |
| POST | `/api/users` | 201 |
| GET | `/api/users/by-email?email=...` | 200 |
| GET | `/api/users/by-username?username=...` | 200 |
| POST | `/api/tickets` | 201 |
| GET | `/api/tickets/{ticketCode}` | 200 |
| GET | `/api/tickets/by-user?email=...` | 200 |
| PATCH | `/api/tickets/{ticketCode}/cancel` | 200 |
| PATCH | `/api/tickets/{ticketCode}/use` | 200 |

### Codigos de error

| Condicion | HTTP |
|-----------|------|
| Bean Validation / JSON mal formado / parametro invalido | 400 |
| Sin identidad valida (sin token, expirado, manipulado, usuario inactivo, login incorrecto) — Fase 4 | 401 |
| Identidad valida sin permiso (rol insuficiente o recurso ajeno) — Fase 4 | 403 |
| `ResourceNotFoundException` | 404 |
| `DuplicateResourceException`, `BusinessRuleException` | 409 |
| Error inesperado (sin exponer detalles internos) | 500 |

Ejemplo de `ErrorResponse`:

```json
{
  "timestamp": "2026-10-04T20:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": { "eventCode": "Event code is required" }
}
```

### Pruebas de Controller

Las pruebas usan `@WebMvcTest`, `MockMvc` y `@MockitoBean` (Services simulados): no requieren Docker ni PostgreSQL.

```bash
./mvnw test -Dtest='*ControllerTest'
```

Para ejecutar toda la suite (incluye los `*IT` con Testcontainers, requiere Docker):

```bash
./mvnw clean test
```

## Preguntas para el equipo

## 1. Por qué Ticket debe ser una entidad en lugar de un @ManyToMany entre User y Event?

Porque Ticket posee atributos y un ciclo de vida propio, que no se pueden representar unicamente con una tabla intermedia de una relacion `@ManyToMany` simple. Un ticket tiene datos de negocio esenciales como un código único (`ticketCode`), un tipo (`type`), un precio (`price`), un estado (`status`) y una fecha de compra (`purchaseDate`). Al modelarlo como entidad propia (`Ticket`), permitimos que un usuario pueda comprar múltiples entradas y que un evento venda múltiples entradas manteniendo trazabilidad individual sobre cada transacción.

## 2. ¿Qué reglas pertenecen a PostgreSQL y cuáles deberían quedar para una futura capa Service?

Las reglas pertenecientes a PostgreSQL son las estrictas de identidad y consistencia estructuralque deben cumplirse a nivel motor sin importar desde dónde se invoque (ej: `UNIQUE`, `CHECK`).

Las reglas para una capa Service son aquellas reglas complejas, orquestaciones o validaciones secuenciales que dependen del estado del sistema o de la lógica de la aplicación (ej: verificar si un evento está publicado antes de permitir la interacción, calcular disponibilidad contra políticas comerciales dinámicas o coordinar transacciones complejas).

## 3. ¿Qué consultas pueden expresarse claramente como Query Methods y cuáles justifican JPQL?

- **Query Methods**: Se utilizan para consultas sencillas y directas donde el parser de Spring Data JPA puede deducir la sentencia SQL fácilmente y el nombre sigue siendo legible.

- **JPQL**: Se justifica cuando la consulta involucra múltiples uniones (``JOIN``), filtrados avanzados combinados (ej: buscar eventos por ciudad y artista simultáneamente), proyecciones personalizadas, o funciones de agregación complejas (`COUNT`, `AVG`) donde un Query Method resultaría excesivamente largo, ilegible o ineficiente.

## 4. ¿Qué consecuencias tendría modificar V1 después de haberla aplicado en un ambiente compartido?

Rompería por completo el mecanismo de control de versiones de Flyway. Flyway calcula un código hash (`checksum`) de cada script de migración al aplicarlo y lo almacena en la tabla de historial (`flyway_schema_history`). Si un archivo ya ejecutado (como `V1`) se modifica posteriormente, el checksum cambiará al intentar correrse en otro entorno (como producción o el equipo de un compañero), lo que provocará un error de validación e impedirá que la aplicación arranque. Cualquier cambio estructural posterior debe hacerse mediante una nueva migración (ej. `V4, V5`).

## 5. ¿Qué diferencias podría ocultar una prueba con H2 frente a PostgreSQL?

H2 es una base de datos en memoria que emite comportamientos SQL simplificados y puede ocultar diferencias críticas de dialecto, tipos de datos (como restricciones `CHECK` avanzadas, tipos `UUID` o manejo de timestamps), y diferencias en la concurrencia o en el comportamiento de transacciones frente a **PostgreSQL** real. Por esta razón, el uso de **Testcontainers** garantiza un entorno de pruebas real y reproducible sin las falsas sensaciones de éxito que ofrece H2.

## 6. ¿Como evolucionaria el modelo para soportar inventario de tickets y evitar sobreventa?

El modelo actual actual asume una capacidad global por venue. Para evolucionarlo y evitar la sobreventa:

- Se introduciria una entidad Sector/Zona (ej: `TicketSection`) vinculada al evento con una capacidad especifica por tipo (VIP, General, etc.).

- Se separaría el proceso de selección temporal mediante un estado de bloqueo (*RESERVED*) con expiración.

- Se aplicaría control de concurrencia a nivel de base de datos (bloque optimista o pesimista, por ejemplo `@Version`) para garantizar que dos usuarios no compren simultáneamente el último asiento disponible.



## Capa de Seguridad (Fase 4): Spring Security + JJWT

API **stateless** con JWT firmado (HS256, JJWT 0.13), contraseñas con BCrypt y tres roles (`USER`, `STAFF`, `ADMIN`).
Se conservan las 20 rutas, los DTOs y las reglas de negocio de las fases 1–3.

### Variables de entorno

| Variable | Obligatoria | Descripción |
|----------|-------------|-------------|
| `PULSEPASS_JWT_SECRET_BASE64` | **Sí** | Base64 de al menos 32 bytes aleatorios. La app **no arranca** si falta o es débil. Genérela con `openssl rand -base64 32`. |
| `PULSEPASS_JWT_ISSUER` | No | Emisor esperado (por defecto `pulsepass-api`). |
| `PULSEPASS_SEED_ADMIN_PASSWORD` / `PULSEPASS_SEED_STAFF_PASSWORD` | Solo perfil `local-seed` | Contraseñas (mín. 12 caracteres) de las cuentas de demostración `admin` y `laura.staff`. |
| `PULSEPASS_CORS_ALLOWED_ORIGINS` | No | Orígenes CORS **explícitos** separados por coma. Vacío (por defecto) = CORS desactivado. Un comodín (`*`) hace fallar el arranque. |
| `PULSEPASS_ACTIVATE_USERNAME` / `PULSEPASS_ACTIVATE_PASSWORD` | Solo perfil `legacy-activation` | Usuario preexistente a activar y su contraseña (12–72 bytes). |

Plantilla en `.env.example` (no versionar `.env`). La duración del token es `pulsepass.security.jwt.access-token-ttl` (por defecto `PT15M` = 900 s).

```bash
export PULSEPASS_JWT_SECRET_BASE64=$(openssl rand -base64 32)
export PULSEPASS_SEED_ADMIN_PASSWORD='UnaFraseAdminLarga2026!'
export PULSEPASS_SEED_STAFF_PASSWORD='UnaFraseStaffLarga2026!'
./mvnw spring-boot:run -Dspring-boot.run.profiles=local-seed   # crea admin y laura.staff (solo desarrollo)
```

Los roles `STAFF`/`ADMIN` **nunca** se asignan por HTTP: el registro público siempre crea `ROLE_USER`.

### Activación de usuarios preexistentes (legados)

La migración `V4` deja a los usuarios anteriores a la Fase 4 con rol `USER` pero **sin credenciales**: no pueden iniciar sesión y **no se les asigna ninguna contraseña por defecto ni compartida**. Para activar a un usuario concreto de forma controlada:

```bash
export PULSEPASS_JWT_SECRET_BASE64=$(openssl rand -base64 32)
export PULSEPASS_ACTIVATE_USERNAME='usuario-legado'
export PULSEPASS_ACTIVATE_PASSWORD='UnaFraseUnicaParaEsteUsuario2026!'
./mvnw spring-boot:run -Dspring-boot.run.profiles=legacy-activation   # detener la app tras ver el log de activación
```

El procedimiento (`LegacyUserActivationRunner`) solo activa usuarios existentes **sin** credencial (nunca sobrescribe un hash), valida la contraseña, guarda únicamente el hash BCrypt, **nunca** asigna `STAFF`/`ADMIN` y no registra la contraseña. Como el MVP aún no incluye "cambio de contraseña", entregue la contraseña por un canal seguro.

### Trazabilidad y CORS

* Cada request recibe un **`X-Request-Id`** (se reutiliza el entrante solo si tiene formato seguro; si no, se genera un UUID). Aparece en la respuesta y en todos los logs (`[requestId]`), incluidos login correcto/fallido y denegaciones 401/403, sin registrar contraseñas, hashes ni tokens.
* **CORS** se configura con `PULSEPASS_CORS_ALLOWED_ORIGINS` (orígenes explícitos, sin credenciales). Con el valor por defecto (vacío) no se permite ningún origen cruzado.

### Endpoints nuevos y cambios de contrato

| Método | Ruta | Acceso | Resultado |
|--------|------|--------|-----------|
| POST | `/api/auth/login` | Público | 200 `{accessToken, tokenType:"Bearer", expiresIn:900}`; 401 genérico si falla |
| GET | `/api/auth/me` | Bearer | 200 `{id, username, email, roles}` |
| POST | `/api/users` | Público | 201 `UserResponse`. **`RegisterUserRequest` ahora exige `password`** (12–72 caracteres/bytes) |

```bash
curl -X POST localhost:8080/api/users -H 'Content-Type: application/json' -d '{"username":"andrea","email":"andrea@mail.com","firstName":"Andrea","lastName":"Lopez","birthDate":"1995-05-20","password":"UnaFraseSeguraLarga2026!"}'
curl -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"identifier":"andrea","password":"UnaFraseSeguraLarga2026!"}'
curl localhost:8080/api/auth/me -H "Authorization: Bearer <accessToken>"
```

### Matriz de autorización

| Operación | Acceso |
|-----------|--------|
| `GET /api/venues/{code}`, `/api/venues/active`, `/api/artists/**` | Público |
| `GET /api/events/published` | Público |
| `GET /api/events/{eventCode}` | Público solo si el evento está `PUBLISHED`; otros estados: `STAFF`/`ADMIN` (anónimo → 401, USER → 403) |
| `GET /api/events/by-artist` | Público; la lista se **filtra**: solo eventos `PUBLISHED` (`STAFF`/`ADMIN` ven todos los estados) |
| `POST /api/events`, `PATCH /api/events/{c}/publish`, `POST /api/events/{c}/artists/{id}` | `ADMIN` |
| `GET /api/events/{c}/tickets/paid`, `PATCH /api/tickets/{t}/use` | `STAFF`/`ADMIN` |
| `POST /api/tickets` | `USER`; `userEmail` debe ser el del principal |
| `GET /api/tickets/{t}` | Propietario, `STAFF`/`ADMIN` |
| `GET /api/tickets/by-user`, `GET /api/users/by-email`, `GET /api/users/by-username` | El propio usuario o `ADMIN` |
| `PATCH /api/tickets/{t}/cancel` | Propietario o `ADMIN` (más las reglas del Service) |

* Rutas sin regla explícita: **denegadas por defecto** (`anyRequest().authenticated()`).
* **Anti-IDOR**: la propiedad se verifica en los Services con `@PreAuthorize("@authz...")` contra el **ID persistente** del principal (`AuthorizationService`), nunca contra datos del request.
* **Política de objeto ajeno**: 403 consistente (también para un ticket inexistente consultado por un `USER`, para no revelar existencia).
* `401` = sin identidad válida (sin token, expirado, manipulado, usuario inactivo); `403` = identidad válida sin permiso. Ambos usan `ErrorResponse`.
* Estado y roles se **revalidan contra la base de datos en cada request**: desactivar un usuario o quitarle un rol surte efecto de inmediato, aunque su JWT siga vigente.

### Limitaciones conocidas del MVP (no implementadas)

Refresh tokens, revocación por `jti`/logout con denylist, límite de intentos de login, rotación de claves, MFA y recuperación de contraseña. `STAFF` es global (el dominio aún no asigna personal a eventos): restringirlo por evento antes de producción. Use HTTPS fuera de localhost y un gestor de secretos.

### Pruebas

```bash
./mvnw clean test
```

Incluye `JwtServiceTest`, `JwtAuthenticationFilterTest`, `CustomUserDetailsServiceTest`, `RequestIdFilterTest`, `SecurityConfigCorsTest`, `AuthServiceTest`, `AuthorizationServiceTest`, `LegacyUserActivationRunnerTest`, `SecurityRulesTest` y `CorsConfigTest` (MockMvc con la cadena de filtros real), más `SecurityFlowIT` y `FlywayMigrationIT` (PostgreSQL con Testcontainers: login real, anti-IDOR, borradores ocultos, migración V4 sobre esquema nuevo y ya migrado). El perfil de pruebas usa una clave HMAC ficticia en `src/test/resources/config/application.properties`.
