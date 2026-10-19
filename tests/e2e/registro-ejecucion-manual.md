# Registro de ejecución E2E manual

## Datos de la ejecución

| Campo | Valor |
|---|---|
| Proyecto | PulsePass |
| Documento guía | `PRD_PulsePass_Pruebas_E2E_Postman_Manual.pdf` |
| Fechas | 2026-10-18 a 2026-10-19 (America/Bogota) |
| Herramienta | Postman Desktop, envío manual |
| API | `http://localhost:8080` |
| Base de datos | `pulsepass_e2e` |
| Run ID | `r20261018a` |
| Estado | PARCIAL; ver pendientes en `checklist-e2e-manual.md` |

No se registran passwords ni JWT en esta bitácora. Los IDs y códigos son fixtures de desarrollo local.

## Fixtures

| Actor/recurso | Identificador | Estado/rol observado |
|---|---|---|
| USER_A | ID 1, `e2e_user_a_20261018` | USER, activo |
| USER_B | ID 2, `e2e_user_b_20261018` | USER, activo |
| ADMIN | ID 3, `e2e_admin_20261018` | ADMIN, activo |
| STAFF | ID 4, `e2e_staff_20261018` | STAFF, activo |
| Venue | `VEN-E2E-R20261018A` | Activa |
| Artist | ID 3, Caribbean Sound | Activo |
| Event | `PP-DRAFT-r20261018a` | DRAFT |
| Event | `PP-PUB-r20261018a` | PUBLISHED |
| Ticket | ID 1 (`ticketCancelCode`) | CANCELLED |
| Ticket | ID 2 (`ticketUseCode`) | USED |

## Resultados observados

| Fecha | Actor | Ruta/caso | Esperado | Observado | Estado / evidencia |
|---|---|---|---:|---:|---|
| 2026-10-18 | Anónimo | GET `/api/venues/{code}` | 200 | 200 | PASS; sede activa |
| 2026-10-18 | Anónimo | GET `/api/venues/active` | 200 | 200 | PASS; lista activa |
| 2026-10-18 | Anónimo | GET artista por ID, nombre y activos | 200 | 200 | PASS; cinco artistas activos |
| 2026-10-18 | Público | POST `/api/users` USER_A / USER_B | 201 | 201 | PASS; IDs 1 y 2, sin password en respuesta |
| 2026-10-18 | Admin local | Registrar y promover ADMIN/STAFF | 201 + roles | 201 + roles | PASS; IDs 3 y 4; `/me` confirmó roles |
| 2026-10-18 | USER_A | GET perfil propio por email/username | 200 | 200 | PASS |
| 2026-10-18 | USER_A | Consultar USER_B por email/username | 403 | 403 | PASS |
| 2026-10-19 | USER_B | Consultar USER_A por email/username | 403 | 403 | PASS |
| 2026-10-19 | ADMIN | Consultar USER_A por username | 200 | 200 | PASS |
| 2026-10-19 | No Auth | GET `/api/auth/me` | 401 | 401 | PASS |
| 2026-10-19 | Bearer mal formado | GET `/api/auth/me` | 401 | 401 | PASS |
| 2026-10-19 | USER_A | Login con password incorrecta | 401 | 401 | PASS en estado; comparar mensaje pendiente |
| 2026-10-19 | Identificador inexistente | Login | 401 | 401 | PASS en estado; comparar mensaje pendiente |
| 2026-10-19 | Público | Registro con correo inválido | 400 | 400 | PASS |
| 2026-10-19 | Público | Registro con username duplicado | 409 | 409 | PASS después de quitar Authorization residual |
| 2026-10-18 | ADMIN | Crear y consultar evento DRAFT | 201 / 200 | 201 / 200 | PASS; `PP-DRAFT-r20261018a` |
| 2026-10-18 | USER_A | Leer borrador / asociar artista | 403 / 403 | 403 / 403 | PASS |
| 2026-10-18 | Anónimo | Leer borrador | 401 | 401 | PASS; acceso denegado |
| 2026-10-19 | USER_A | Crear evento con payload válido | 403 | 403 | PASS; GET como ADMIN dio 404, no se creó |
| 2026-10-18 | ADMIN | Asociar artista y publicar evento | 200 / 200 | 200 / 200 | PASS; evento PUBLISHED |
| 2026-10-18 | Público | Lista de publicados, evento por código, búsqueda por artista | 200, ocultar DRAFT | 200 | PASS; publicado visible y borrador oculto |
| 2026-10-18 | USER_A | Comprar ticket propio y consultar/listar | 201 / 200 | 201 / 200 | PASS; ticket ID 1 PAID inicialmente |
| 2026-10-18 | USER_A | Comprar para USER_B | 403 | 403 | PASS |
| 2026-10-18 | USER_B | Leer/listar/cancelar ticket ajeno | 403 | 403 | PASS |
| 2026-10-18 | USER_A | Cancelar ticket propio; repetir cancelación | 200 / 409 | 200 / 409 | PASS; ID 1 quedó CANCELLED y GET lo confirmó |
| 2026-10-18 | USER_A / STAFF | Listar tickets pagados | 403 / 200 | 403 / 200 | PASS |
| 2026-10-18 | USER_A | Segundo ticket comprado | 201 | 201 | PASS; ticket ID 2 PAID |
| 2026-10-18 | USER_A / USER_B | Usar ticket ID 2 | 403 / 403 | 403 / 403 | PASS |
| 2026-10-18 | STAFF | Marcar ID 2 como usado y repetir | 200 / 409 | 200 / 409 | PASS; GET USER_A y STAFF confirmó USED |

## Casos todavía pendientes

- STAFF intenta publicar `PP-DRAFT-r20261018a` y comprobar que sigue DRAFT.
- Repetir asociación del artista al mismo evento y comprobar 409/no duplicación.
- STAFF/ADMIN consultan búsqueda por artista con DRAFT.
- Login válido por email y comparación del mensaje para identificador desconocido vs password incorrecta.
- Tokens alterado, en query string, vencido/de otra clave; usuario inactivo con token anterior.
- Inspección explícita del esquema de los errores 400/401/403/409 y ausencia de datos secretos.
- Caso de lectura/cancelación de ticket por ADMIN según la matriz.

## Incidentes de configuración y regresiones

| Incidente | Observación | Clasificación |
|---|---|---|
| Variable de URL | La variable escrita como `{{baseurl}}` no resolvía; al usar URL literal las llamadas locales funcionaron. La colección entregable normaliza el nombre a `{{baseUrl}}`. | Configuración de Postman; validar resolución tras importar. |
| Conectividad | `ECONNREFUSED` mientras la API no estaba disponible; tras iniciar el backend, la API respondió. | Precondición local; no se confirmó defecto del servicio. |
| Autenticación | Varios 401 se resolvieron al corregir/renovar el token o quitar un Authorization residual en una solicitud pública. | Configuración de token/header; revisar vigencia antes de clasificar. |
| Crear evento como USER_A | Hubo un intento 404 y luego una repetición 403 con body válido; GET como ADMIN dio 404 para el código denegado. | 403 final cumple autorización; causa del primer 404 no quedó diagnosticada y no se clasifica como regresión. |

No hay una regresión funcional del backend confirmada por la evidencia comunicada. Las discrepancias no diagnosticadas deben conservarse como observaciones y no marcarse como aprobadas por inferencia.

## Evidencia

Las respuestas fueron comunicadas durante la sesión; no se adjuntaron capturas. Para completar la evidencia, guardar capturas de Postman sin el panel/valor del token ni encabezados Authorization visibles y enlazarlas aquí por ID de caso.
