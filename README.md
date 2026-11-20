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

## Inicio local y acceso de administración

La aplicación no convierte una cuenta registrada en ADMIN automáticamente. Por seguridad, el registro público solo crea cuentas `USER`. Para probar las funciones de administración se debe iniciar el backend con el perfil `local-seed`, que crea una cuenta ADMIN únicamente si se proporciona una contraseña propia mediante variable de entorno.

1. Inicie PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

2. Desde la carpeta raíz del proyecto, configure una clave JWT y una contraseña segura para el administrador (mínimo 12 caracteres):

   **PowerShell:**
   ```powershell
   $bytes = [byte[]]::new(32)
   [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
   $env:PULSEPASS_JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
   $env:PULSEPASS_SEED_ADMIN_PASSWORD = "EscribaAquiUnaClavePropiaDe12Caracteres!"
   ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-seed"
   ```

   **Linux/macOS:**
   ```bash
   export PULSEPASS_JWT_SECRET_BASE64="$(openssl rand -base64 32)"
   export PULSEPASS_SEED_ADMIN_PASSWORD='EscribaAquiUnaClavePropiaDe12Caracteres!'
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local-seed
   ```

3. Inicie el frontend en otra terminal:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

4. Inicie sesión en `/login` con:
   - Usuario: `admin`
   - Contraseña: el valor que configuró en `PULSEPASS_SEED_ADMIN_PASSWORD`.

La cuenta de demostración usa el correo `admin@pulsepass.local`. No existe una contraseña universal. El seed es idempotente: si la cuenta ya existe, agrega los roles que le falten y crea credenciales solo si no existen; nunca sobrescribe una contraseña existente. Si la cuenta está desactivada, debe reactivarse manualmente en la base de datos local.

El panel ADMIN ahora consulta `GET /api/events/admin`, endpoint protegido que lista todos los eventos y sus estados, incluidos los borradores. El resto de usuarios no puede acceder a este endpoint ni asignarse privilegios desde el registro público.

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
| GET | `/api/events/admin` | 200 (solo ADMIN; incluye borradores) |
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

## Frontend (Fase 5)

El cliente React + TypeScript + Tailwind está integrado en `frontend/`.

```bash
cd frontend
npm install
npm run dev
```

Abre `http://localhost:5173`. El proxy de Vite envía `/api` a `http://localhost:8080`; por tanto, el backend Spring Boot debe estar iniciado en una segunda terminal. Los requisitos de Node, los comandos de pruebas y el mapa de rutas están en `frontend/README.md`.


## Administración de sedes y artistas (ADMIN)

Con sesión iniciada como ADMIN, abre **Administración → Gestionar sedes y artistas** (`/admin/catalogos`). Desde allí puedes registrar sedes (código único, nombre, ciudad, dirección y capacidad) y artistas (nombre artístico, país y género). Los registros se crean activos y quedan disponibles en los selectores.

- `POST /api/venues` — alta de sede, solo ADMIN.
- `POST /api/artists` — alta de artista, solo ADMIN.
- `GET /api/venues/active` y `GET /api/artists/active` — catálogos activos para selección pública/autorizada.

Para crear un evento, selecciona una sede activa. Después de crearlo, entra en **Gestionar evento → Artistas asociados** para vincular uno o varios artistas. El selector excluye los artistas que ya estén asociados. En el formulario de creación también puedes usar **+ Nueva** para registrar una sede sin abandonar la pantalla; al guardarla queda seleccionada automáticamente.


## Flujo de compra del MVP académico

La compra de entradas es parte obligatoria del frontend. El usuario debe iniciar sesión con rol `USER`, abrir un evento publicado, seleccionar el tipo de entrada (`GENERAL`, `VIP`, `BACKSTAGE` o `STUDENT`) y confirmar. El frontend envía `POST /api/tickets`; el backend calcula el precio, registra el ticket y devuelve su código, precio y estado. La pantalla de confirmación permite abrir el ticket y consultar **Mis tickets**.

**No hay pasarela de pago ni captura de tarjetas en este MVP:** la integración de pagos está fuera del alcance del PRD de Fase 5. Para probar una compra, el evento debe estar en estado `PUBLISHED`, tener fecha futura y contar con capacidad disponible. Si existe edad mínima, el perfil del usuario debe tener fecha de nacimiento y cumplirla. Los rechazos de reglas de negocio se devuelven como HTTP 409 y se muestran en el formulario.

Un evento recién creado comienza como `DRAFT`. Inicia sesión como ADMIN, entra a **Administración → Gestionar evento**, asocia artistas si corresponde y pulsa **Publicar evento**. Después podrás comprarlo con una cuenta `USER`.

---

# Anexos de revisiones del proyecto

Los siguientes documentos se integran como historial de cambios y evidencia de revisión. Se conservan sus fechas, alcance y limitaciones de validación.


## Revisiones funcionales y auditorías

# Revisiones aplicadas a PulsePass

## Problemas detectados

1. El proyecto tenía un frontend administrativo limitado: solo podía buscar eventos por código y recordar los creados durante la sesión. No existía una consulta persistente para listar todos los eventos y borradores.
2. La cuenta ADMIN no se crea al registrarse normalmente. El backend exige el perfil `local-seed` y una contraseña configurada por el desarrollador. Esto no estaba explicado en una guía de inicio rápido.
3. El seed local omitía por completo una cuenta `admin` preexistente, incluso si le faltaban roles o credenciales.
4. El componente de guarda de roles se había definido esperando `children`, pero React Router entrega las rutas hijas mediante `<Outlet />`; esto generaba errores de TypeScript y podía romper las rutas protegidas.
5. El chequeo estricto de TypeScript también detectó un tipo de tupla implícito en los filtros de Mis tickets.

## Cambios aplicados

- Nuevo endpoint `GET /api/events/admin`, protegido exclusivamente para `ADMIN`, que lista todos los eventos incluyendo `DRAFT`.
- Nueva consulta en `EventRepository` y `EventService`, con autorización en la capa de servicio.
- Regla de Spring Security específica para `/api/events/admin`, colocada antes del matcher público de eventos.
- El panel de administración ahora lista todos los eventos, permite filtrar por estado y abrir su gestión. Ya no depende de códigos guardados solo en `sessionStorage`.
- Crear y publicar eventos invalida las consultas del panel para mantener la información actualizada.
- El seed `local-seed` ahora es idempotente: agrega roles faltantes y crea credenciales solo si no existen; nunca reemplaza una contraseña existente.
- Se corrigieron las guardas de rutas para usar `<Outlet />` y esperar a que la sesión termine de cargarse.
- Se corrigió el tipo estricto de los filtros de Mis tickets.
- Se amplió el README con instrucciones de inicio y creación segura de la cuenta ADMIN.
- Se agregaron pruebas de controlador para el listado administrativo y el rechazo a usuarios USER.

## Cómo habilitar ADMIN localmente

El registro público crea únicamente `USER`, deliberadamente. Para crear/habilitar la cuenta de demostración `admin`, consulta la sección **Inicio local y acceso de administración** del `README.md` y ejecuta el backend con el perfil `local-seed`, definiendo `PULSEPASS_JWT_SECRET_BASE64` y `PULSEPASS_SEED_ADMIN_PASSWORD` (mínimo 12 caracteres).

Usuario: `admin`  
Correo: `admin@pulsepass.local`  
Contraseña: la que se defina en `PULSEPASS_SEED_ADMIN_PASSWORD`.

No hay una contraseña predeterminada. El seed no cambia la contraseña de una cuenta existente.

## Validación realizada

- `tsc -b`: correcto después de las correcciones del frontend.
- `vite build`: no se pudo completar en este entorno porque el paquete de dependencias del ZIP no contiene el binario opcional de Rollup para Linux (`@rollup/rollup-linux-x64-gnu`).
- Pruebas Java/Maven: no se pudieron ejecutar porque Maven Wrapper no pudo descargar Maven desde Maven Central en este entorno. Las pruebas nuevas quedaron añadidas a `EventControllerTest`.

En el equipo local, instala dependencias desde `frontend` con `npm ci` usando Node `22.22+`, y ejecuta `npm run build` y `npm test`. En la raíz ejecuta `./mvnw clean test` (o `mvnw.cmd clean test` en Windows, con Docker disponible para los tests de integración).


## Ampliación posterior: catálogos ADMIN

Se detectó que el PRD original contemplaba seleccionar sedes y artistas existentes, pero no contemplaba altas de catálogo. Si la base de datos no tenía sedes activas, el formulario de evento no podía completarse. Se amplió el backend y frontend con:

- `POST /api/venues` protegido para ADMIN, con validación y código único.
- `POST /api/artists` protegido para ADMIN, con validación y nombre artístico único (sin distinguir mayúsculas).
- Pantalla `/admin/catalogos` para registrar y consultar sedes/artistas activos.
- Alta rápida de sede integrada dentro del formulario de evento: no abre otra pestaña ni pierde la sesión; al guardar, la sede queda seleccionada automáticamente.
- En gestión de evento se pueden crear artistas desde el catálogo y el selector excluye los artistas ya asociados.
- Pruebas MockMvc añadidas para alta ADMIN y rechazo de USER en ambos endpoints.

La compilación TypeScript se valida con `npx tsc -b`. Las pruebas Java requieren ejecutar Maven en el entorno local.


## Ajustes de compra y experiencia visual

- Se confirmó en el PRD que la compra de tickets es un requisito Must: seleccionar tipo (`GENERAL`, `VIP`, `BACKSTAGE`, `STUDENT`), enviar `POST /api/tickets` con el correo del usuario principal y redirigir a confirmación; los errores `409` deben mostrarse al usuario.
- La pasarela de pagos está expresamente fuera del alcance del MVP académico. La compra registra la entrada en el backend; no debe pedir datos de tarjeta ni simular una pasarela externa. El backend calcula el precio y devuelve el valor en `TicketResponse`.
- Se mejoró el panel de compra del detalle del evento con estados claros para borrador, agotado, cancelado y finalizado; validación de rol `USER`; información previa al usuario y mensajes de error de negocio. La disponibilidad final sigue siendo validada por el backend.
- Se mejoró la jerarquía visual y el formulario de creación de evento, incluyendo alta rápida de sede en contexto.
- El evento debe estar `PUBLISHED` antes de admitir compras. Al crearlo, su estado inicial es `DRAFT`; el administrador debe publicarlo desde **Gestionar evento**.

## Sincronización de tickets pagados (17-11-2026)

- La compra exitosa ahora invalida también la consulta `['tickets', 'pagados', eventCode]`, además de la lista del comprador y el detalle del evento.
- La pantalla de tickets pagados actualiza la consulta cada 10 segundos y al recuperar el foco de la ventana; incorpora botón de actualización manual, contador y mejor identificación del evento.
- Se aclara que el listado incluye únicamente tickets en estado `PAID`. Al validar un ticket, pasa a `USED` y deja de aparecer en esta lista, de acuerdo con la lógica del backend.
- El backend ya exponía `GET /api/events/{eventCode}/tickets/paid`, filtraba por `TicketStatus.PAID` y autorizaba los roles `STAFF` y `ADMIN`; no se modificaron las reglas de negocio.
- Validación pendiente en el equipo local: compra real con usuario USER y consulta con ADMIN/STAFF sobre el mismo código de evento y la misma base de datos.

## Auditoría integral del PRD y accesibilidad visual (17-11-2026)

Se volvió a revisar el PRD completo de 63 páginas y se contrastó con el frontend existente. Se conservó el trabajo previo y se agregaron los siguientes ajustes:

- Selector de modo claro/oscuro en escritorio y móvil, persistencia de preferencia y detección inicial de la preferencia del sistema.
- Variables CSS para superficies, texto, bordes, formularios y encabezado; corrección de contraste en badges, botones de peligro, mensajes y enlaces. Se alinearon Poppins/Inter y tokens de color con la sección 9.1 del PRD.
- Modal accesible reutilizable para compra, cancelación y publicación, con cierre mediante Escape y gestión básica del foco.
- Cancelación disponible exclusivamente cuando el ticket está `PAID`; confirmación en modal y actualización de las cachés de tickets propios, detalle y tickets pagados del evento.
- Barra fija inferior para compra en móvil y presentación de tickets pagados como tabla en escritorio y tarjetas en móvil.
- Mejoras de accesibilidad en el menú móvil, validación de ingreso, estados de carga y mensajes de sesión expirada.
- Errores HTTP 400 del registro y creación de eventos se asignan a campos cuando la API devuelve detalles que coinciden con el formulario.
- Se extrajeron del PDF y añadieron a `docs/diseno/` las 32 imágenes de referencia que el PRD indexa en la sección 9.0.
- Se amplió la suite a 18 pruebas Vitest de HTTP, esquemas, formatos, componentes y validación de formularios.

### Verificación y limitaciones de este entorno

- `tsc -b` terminó correctamente y ESLint terminó sin errores ni advertencias.
- No se pudo ejecutar Vite build ni la suite Vitest: el entorno tiene Node 22.16.0 (el PRD requiere 22.22+) y falta el binario opcional `@rollup/rollup-linux-x64-gnu` en las dependencias recuperadas. El registro npm tampoco estuvo accesible para instalarlo.
- El PRD solicita MSW para simular la API. No se añadió una dependencia incompleta ni se modificó el lockfile sin poder descargarla; las pruebas nuevas usan mocks de `fetch`. La incorporación de MSW y la matriz completa de pruebas de la sección 21 quedan identificadas como pendientes en `docs/AUDITORIA_PRD_PULSEPASS.md`.
- La auditoría no afirma que el build, lint, las pruebas completas ni la integración con PostgreSQL hayan pasado. Deben ejecutarse en el equipo local con Node 22.22+.


## Segunda auditoría integral contra el PRD (17-11-2026)

Detalle completo en `docs/AUDITORIA_PRD_PULSEPASS.md` (sección «Segunda revisión»). Resumen:

- Corregido el bug que ocultaba mensajes como «Credenciales inválidas» y «El usuario o correo ya está registrado».
- Corregida la carrera de redirección tras el login (`SoloAnonimo` vs `Login`) y alineado el destino por defecto con el PRD (`/`).
- Panel de compra oculto para STAFF/ADMIN; 403/404 de evento y de ticket ajeno con pantallas neutras; mejoras en validación de ingreso.
- `Pages.tsx` dividido en un módulo por página con carga diferida real; guardas en `layouts/`; `errorElement` seguro.
- ~50 pruebas nuevas (guardas, auth, compra, tickets, STAFF, ADMIN).
- Backend: revisado `SecurityConfig` (orden de matchers, `/api/events/admin` antes del patrón público) sin cambios.

**Antes de entregar, en tu equipo (Node 22.22+):** `cd frontend && npm ci && npx tsc -b && npm run lint && npm test && npm run build`. Esto no pudo ejecutarse aquí.

## Ajustes de responsive, contraste y arquitectura (17-11-2026)

- Se reorganizaron los tokens de tema en `frontend/src/index.css`: texto, superficies, fondos, campos, placeholders, bordes, estados y botones ahora cambian con `data-theme`.
- Se reforzaron colores de texto semánticos en modo oscuro para evitar texto oscuro sobre superficies oscuras y textos claros sobre superficies claras.
- Se actualizó el menú principal para que en móvil incluya las rutas públicas, los enlaces según rol, el selector de tema y las acciones de cuenta/salida.
- Se ajustó Inicio a la referencia visual principal `docs/diseno/Main.png` y se rediseñaron las tarjetas de eventos para usar paneles por categoría legibles en ambos temas.
- Se incorporaron hooks de consultas y mutaciones por funcionalidad en `src/features/`; las páginas delegan el estado de servidor y las invalidaciones de caché en esos hooks.
- Se añadieron ajustes de layout para pantallas estrechas y se preservó la presentación móvil de tickets pagados como tarjetas.

**Verificación:** todos los archivos TS/TSX pasan el análisis sintáctico con el transpilador global y no se detectaron imports relativos rotos. No se pudo instalar dependencias: `npm ci --offline` falla por paquetes no disponibles en caché; Node es 22.16.0 y el PRD exige 22.22+. Por eso `npm run build`, `npm test` y `npm run lint` no se pudieron ejecutar. MSW, auditoría axe/Lighthouse, comparación visual completa de los 32 artboards y pruebas integradas con PostgreSQL siguen pendientes. Ver `docs/AUDITORIA_PRD_PULSEPASS.md`.

## Revisión visual según el PRD

# Ajustes visuales de PulsePass según el PRD

Se aplicaron cambios exclusivamente de presentación, conservando la lógica de negocio, las rutas, las llamadas API y los flujos existentes.

## Cambios realizados
- Se restauraron los tokens de marca del PRD: `#6366F1`, `#4F46E5`, `#4338CA` y acento `#F43F5E`.
- Se ajustó la superficie clara a Slate 50 y el tema oscuro a fondo `#030817`, superficies `#0F172A` y bordes Slate.
- Se reemplazaron los glifos de las tarjetas de eventos por iconos SVG escalables para categorías, fecha y ubicación.
- Se reemplazó el símbolo tipográfico del logo por un icono SVG de nota musical.
- Se ajustó el ancho máximo del contenedor principal, encabezado y pie a `max-w-6xl`.
- Se retiró el enlace adicional «Inicio» de la navegación de escritorio; se conserva en el menú móvil.
- Se armonizó el degradado principal con los colores de marca.
- Se sincronizó el color del navegador con los temas claro y oscuro.

## Verificación
La compilación no pudo completarse en este entorno: no existe `node_modules` y la instalación de dependencias no está disponible. `npm run build` se detuvo por ausencia de tipos y paquetes (`vite/client`, `vitest/globals`, `vitest/config`, `@vitejs/plugin-react` y `@tailwindcss/vite`). La versión local de Node es `22.16.0`; el proyecto requiere una versión más reciente según su configuración. Por tanto, estos cambios no se presentan como compilados ni como pruebas aprobadas.

## Alcance
Los cambios de esta revisión son visuales. No se alteraron contratos API, autenticación, permisos, consultas, mutaciones ni persistencia.
