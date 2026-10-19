# Checklist E2E manual

**Convenciones:** `PASS` = resultado observado coincide; `FAIL` = discrepancia; `BLOCKED` = no ejecutable con el entorno disponible; `NOT RUN` = falta ejecutar o revisar evidencia.

## Estado de aprobación al 2026-10-19

- **Rutas nominales:** 22/22 reportadas como enviadas manualmente; los resultados comunicados constan en la bitácora.
- **Paquete documental:** aprobado por revisión estática; la importación real en Postman Desktop aún no está verificada.
- **Aceptación total del PRD:** no aprobada todavía. Los casos `NOT RUN`, `BLOCKED` y `PARTIAL` de esta checklist siguen pendientes; no deben contarse como PASS.

## Preparación y seguridad de artefactos

| ID | Verificación | Resultado |
|---|---|---|
| SET-01 | API local responde en `http://localhost:8080` y PostgreSQL usa `pulsepass_e2e`. | PASS |
| SET-02 | Environment activo y variables case-sensitive; `baseUrl` resuelto. | NOT RUN: solicitudes previas usaron URL literal; validar la colección importada con `{{baseUrl}}`. |
| SET-03 | No compartir tokens, contraseñas, hashes ni encabezados Authorization en exports/evidencias. | PASS en los JSON entregados; revisar valores locales de Postman antes de exportar otros archivos. |
| SET-04 | ADMIN y STAFF se provisionan localmente; el registro HTTP asigna únicamente USER. | PASS |
| SET-05 | Las 22 rutas están en la colección; no contiene scripts Pre-request/Tests ni requiere Runner/Newman. | PASS en revisión estática. |
| HTTP-01 | Requests aplicables envían `Accept: application/json`; cuerpos JSON envían `Content-Type: application/json`. | PASS en la colección entregada. |
| HTTP-02 | JSON inválido, campo obligatorio ausente y enum inválido devuelven el 400 contractual. | NOT RUN; solo se probó correo inválido 400. |

## Venues y artistas

| ID | Caso manual | Esperado | Resultado |
|---|---|---:|---|
| VEN-01 | GET venue por código (`VEN-E2E-R20261018A`). | 200, activa | PASS |
| VEN-02 | GET venues activos. | 200, lista de sedes activas | PASS |
| ART-01 | GET artista ID 3. | 200 | PASS |
| ART-02 | Buscar por stageName `Caribbean Sound`. | 200 | PASS |
| ART-03 | GET artistas activos. | 200, cinco filas observadas | PASS |

## Usuarios y autenticación

| ID | Caso manual | Esperado | Resultado |
|---|---|---:|---|
| USER-01 | Registrar USER_A; respuesta no expone password/hash. | 201 | PASS, ID 1 |
| USER-02 | Registrar USER_B; respuesta no expone password/hash. | 201 | PASS, ID 2 |
| USER-03 | Registrar ADMIN y STAFF; promover roles solo en DB local y verificar `/me`. | 201 y roles correctos | PASS, ADMIN ID 3; STAFF ID 4 |
| USER-04 | Registro con correo inválido. | 400, no crea usuario | PASS |
| USER-05 | Registro con username duplicado, sin Authorization residual. | 409, no duplica usuario | PASS |
| USER-06 | USER consulta su propio email y username. | 200 | PASS |
| USER-07 | USER_A consulta datos de USER_B. | 403 | PASS por email y username |
| USER-08 | USER_B consulta datos de USER_A. | 403 | PASS por email y username |
| USER-09 | ADMIN consulta usuario ajeno. | 200 | PASS por email y username |
| AUTH-01 | Login válido y `/me` para USER_A, USER_B, ADMIN y STAFF. | 200, rol correcto | PASS |
| AUTH-01a | Login con email de USER_A; respuesta incluye accessToken, tokenType Bearer y expiresIn configurado. | 200 | NOT RUN: login por email y metadata completa no quedaron confirmados |
| AUTH-02 | Login con contraseña incorrecta. | 401 genérico | PASS en estado; revisar igualdad del `message` |
| AUTH-03 | Login con identificador inexistente. | 401 genérico | PASS en estado; comparar `message` con AUTH-02 aún pendiente |
| AUTH-04 | `/api/auth/me` sin Authorization. | 401 | PASS |
| AUTH-05 | `/api/auth/me` con Bearer mal formado. | 401 | PASS |
| AUTH-06 | JWT alterado (un carácter cambiado). | 401 | NOT RUN |
| AUTH-07 | JWT expirado. | 401 | BLOCKED: no se preparó un fixture de token de prueba con expiración controlada |
| AUTH-08 | JWT de otra clave/issuer. | 401 | BLOCKED: falta preparar una clave/issuer de prueba aislados; no usar la clave real ni exponer secretos |
| AUTH-09 | Token en query string en lugar de Bearer header. | 401 | NOT RUN |
| AUTH-10 | Bearer inválido enviado a endpoint público. | Aplicar la política del filtro; no autenticarlo | NOT RUN |
| AUTH-11 | INACTIVE_USER con token previo. | Denegado según política | BLOCKED: no se preparó la cuenta inactiva con un token anterior |
| AUTH-12 | Claims `sub`, `iss`, `iat`, `exp`, `jti` presentes; inspección no autoritativa local. | JWT válido | NOT RUN |
| AUTH-13 | Registro intenta inyectar `role=ADMIN`; cuenta rechazada o queda solo USER. | Nunca ADMIN | NOT RUN |
| SEC-017 | Revocar rol de fixture ADMIN/STAFF y reutilizar JWT anterior. | Privilegio revocado según política del servidor | NOT RUN |

## Eventos

| ID | Caso manual | Esperado | Resultado |
|---|---|---:|---|
| EVT-01 | ADMIN crea `PP-DRAFT-r20261018a`. | 201, DRAFT | PASS |
| EVT-02 | ADMIN consulta borrador por código. | 200, DRAFT | PASS |
| EVT-03 | USER_A consulta borrador. | 403 | PASS |
| EVT-04 | Anónimo consulta borrador por código. | 401/denegado, sin filtrar borrador | PASS, 401 |
| EVT-05 | USER_A crea un evento con payload válido. | 403 | PASS; GET ADMIN posterior dio 404 |
| EVT-06 | USER_A intenta asociar artista al borrador. | 403 | PASS |
| EVT-07 | ADMIN asocia artista ID 3 al borrador. | 200 | PASS |
| EVT-08 | ADMIN repite asociación del mismo artista. | 409, estado/asociación sin duplicar | NOT RUN |
| EVT-09 | ADMIN crea `PP-PUB-r20261018a`, asocia artista y publica. | 201 / 200 / 200, termina PUBLISHED | PASS |
| EVT-10 | USER_A intenta publicar el borrador. | 403, sigue DRAFT | PASS; GET ADMIN confirmó DRAFT |
| EVT-11 | STAFF intenta publicar el borrador. | 403, sigue DRAFT | NOT RUN |
| EVT-12 | Anónimo lista publicados y consulta evento publicado. | 200; no incluye DRAFT | PASS |
| EVT-13 | Anónimo busca por artista. | 200; oculta DRAFT | PASS |
| EVT-14 | STAFF/ADMIN buscan por artista con evento DRAFT. | 200; pueden ver estados operativos según política | NOT RUN |
| EVT-15 | Anónimo intenta crear evento con payload válido. | 401 y ningún evento creado | NOT RUN |

## Tickets

| ID | Caso manual | Esperado | Resultado |
|---|---|---:|---|
| TKT-01 | USER_A compra para sí en evento publicado. | 201, PAID | PASS, ID 1 |
| TKT-02 | USER_A intenta comprar para USER_B. | 403, no crea ticket | PASS |
| TKT-03 | Propietario consulta ticket y lista propia. | 200 | PASS |
| TKT-04 | USER_B lee, lista o cancela ticket de USER_A. | 403, sin mutación | PASS; la cancelación legítima posterior dejó ID 1 CANCELLED |
| TKT-05 | USER_A cancela ticket propio PAID. | 200, CANCELLED | PASS |
| TKT-06 | Repetir cancelación de ticket ya CANCELLED. | 409, permanece CANCELLED | PASS; GET posterior confirmó CANCELLED |
| TKT-07 | USER_A lista tickets pagados de evento. | 403 | PASS |
| TKT-08 | STAFF lista tickets pagados de evento. | 200; incluye PAID, excluye CANCELLED | PASS |
| TKT-09 | USER_A y USER_B intentan usar ticket. | 403 | PASS |
| TKT-10 | STAFF marca ticket PAID como usado. | 200, USED | PASS, ID 2 |
| TKT-11 | STAFF repite uso sobre ticket USED. | 409, permanece USED | PASS; GET posterior confirmó USED |
| TKT-12 | STAFF consulta ticket ajeno para operación. | 200 | PASS, ID 2 USED |
| TKT-13 | ADMIN consulta/cancela ticket operativo según matriz. | 200 si aplica | NOT RUN |

## Respuestas de error y cierre

| ID | Verificación | Resultado |
|---|---|---|
| ERR-01 | Respuestas 400/401/403/409 incluyen `timestamp,status,error,message,details`; HTTP coincide con `status`. | NOT RUN: revisar los cuerpos guardados en Postman. |
| ERR-02 | Errores no revelan password/hash/JWT ni datos de otro usuario. | NOT RUN: revisar cuerpos y evidencia. |
| DOC-01 | Bitácora contiene fecha, actor, ruta, esperado, observado y evidencia sin secretos. | PASS como registro parcial; completar filas NOT RUN al cerrar ejecución. |
| DOC-02 | Export de colección y plantilla no contiene credenciales ni tokens. | PASS para los JSON entregados; no exportar valores locales de Postman. |

## Criterio de cierre

No declarar la ejecución completa mientras queden filas `NOT RUN`, `BLOCKED`, `PARTIAL` o `FAIL` sin cierre. En esta corrida siguen pendientes login por email/metadata, AUTH-06, AUTH-09/10/12/13, SEC-017, EVT-08/11/14/15, TKT-13 y la inspección de ErrorResponse. AUTH-07, AUTH-08 y AUTH-11 están BLOCKED por falta de fixtures aislados; no se usaron claves ni datos reales.

## Anexo A — Matriz de las 22 rutas y roles


`200/201` indican éxito; `401` ausencia/credencial inválida; `403` rol o propiedad denegada; `400` validación; `404` recurso inexistente/no filtrado; `409` conflicto de estado o duplicado. La matriz expresa la política esperada del backend; la última columna marca lo observado en esta ejecución.

| # | Método y ruta | Anónimo | USER | STAFF | ADMIN | Observado |
|---:|---|---|---|---|---|---|
| 1 | `GET /api/venues/{code}` | 200 | 200 | 200 | 200 | PASS, público |
| 2 | `GET /api/venues/active` | 200 | 200 | 200 | 200 | PASS, público |
| 3 | `GET /api/artists/{id}` | 200 | 200 | 200 | 200 | PASS, público |
| 4 | `GET /api/artists/by-stage-name?stageName=` | 200 | 200 | 200 | 200 | PASS, público |
| 5 | `GET /api/artists/active` | 200 | 200 | 200 | 200 | PASS, público |
| 6 | `POST /api/users` | 201; 400 inválido; 409 duplicado | Igual; crea solo USER | Igual; crea solo USER | Igual; crea solo USER | PASS: 201, 400 y 409 |
| 7 | `GET /api/users/by-email?email=` | 401 | Propio 200; ajeno 403 | Propio 200; ajeno 403 | Cualquiera 200 | PASS: propio/ajeno y ADMIN |
| 8 | `GET /api/users/by-username?username=` | 401 | Propio 200; ajeno 403 | Propio 200; ajeno 403 | Cualquiera 200 | PASS: propio/ajeno y ADMIN |
| 9 | `POST /api/auth/login` | Válido 200; inválido/no existente 401 | Igual | Igual | Igual | PASS en estados; comparar mensaje genérico pendiente |
| 10 | `GET /api/auth/me` | 401 | 200 propio | 200 propio | 200 propio | PASS; sin auth 401, roles 200 |
| 11 | `POST /api/events` | 401 | 403 | 403 | 201, DRAFT | PASS: ADMIN 201; USER 403, no creado |
| 12 | `GET /api/events/{eventCode}` | Publicado 200; borrador denegado/401 | PUBLISHED 200; DRAFT 403 | DRAFT/PUBLISHED 200 | DRAFT/PUBLISHED 200 | PASS: publicado y borrador; anónimo 401 |
| 13 | `GET /api/events/published` | 200 solo PUBLISHED | 200 solo PUBLISHED | 200 solo PUBLISHED | 200 solo PUBLISHED | PASS; borrador excluido |
| 14 | `PATCH /api/events/{eventCode}/publish` | 401 | 403 | 403 | DRAFT→PUBLISHED 200 | Parcial: ADMIN y USER; STAFF pendiente |
| 15 | `POST /api/events/{eventCode}/artists/{artistId}` | 401 | 403 | 403 | 200; duplicado 409 | Parcial: ADMIN 200, USER 403; duplicado pendiente |
| 16 | `GET /api/events/by-artist?stageName=` | 200 solo publicados | 200 solo publicados | 200 incluye estados operativos | 200 incluye estados operativos | Parcial: anónimo probado; STAFF/ADMIN pendientes |
| 17 | `POST /api/tickets` | 401 | Propio 201; comprar para otro 403 | 403 | 403 | PASS en USER propio/ajeno |
| 18 | `GET /api/tickets/{ticketCode}` | 401 | Propio 200; ajeno 403 | 200 operativo | 200 operativo | PASS: owner, USER ajeno 403 y STAFF 200 |
| 19 | `GET /api/tickets/by-user?email=` | 401 | Propio 200; ajeno 403 | Propio si aplica; ajeno 403 | Cualquiera 200 | PASS: owner/otro USER; ADMIN por usuario verificado parcialmente |
| 20 | `GET /api/events/{eventCode}/tickets/paid` | 401 | 403 | 200 | 200 | PASS: USER 403, STAFF 200 |
| 21 | `PATCH /api/tickets/{ticketCode}/cancel` | 401 | Propio 200; ajeno 403; repetido 409 | No propietario 403 | 200 según política | PASS: owner, foreign USER y conflicto; ADMIN pendiente |
| 22 | `PATCH /api/tickets/{ticketCode}/use` | 401 | 403 | 200; repetido 409 | 200; repetido 409 | PASS: USER_A/USER_B 403, STAFF 200/409 |

## Reglas operativas de la matriz

- Todas las solicitudes que requieren token usan `Authorization: Bearer <token>` desde Postman **Authorization → Bearer Token**; no duplicar el header manualmente.
- JWT vence aproximadamente a los 15 minutos. Si aparece 401 durante una prueba con token, renovar login y repetir la solicitud antes de clasificar el caso.
- Para revisar denegaciones, enviar un GET posterior con actor autorizado y confirmar que no hubo mutación.
- El endpoint `/api/users/by-user` no forma parte del backend; el nombre correcto es `/api/tickets/by-user?email=...`.


## Anexo B — Trazabilidad FR-E2E y aceptación

## Requisito funcional a archivo/caso/resultado

| Requisito | Archivo/caso que lo cubre | Estado de documentación | Estado de ejecución |
|---|---|---|---|
| FR-E2E-001 colección importable y 22 rutas | `postman/PulsePass-Manual-E2E.postman_collection.json`; Anexo A, rutas #1–22 | PASS estructural: 22 requests, JSON válido; importación desde UI aún no observada | PASS nominal reportado para las 22 rutas; escenarios adicionales parciales |
| FR-E2E-002 entorno con base URL/datos/tokens | `postman/PulsePass-local.postman_environment.json` | PASS estático: `baseUrl` y variables; valores secretos vacíos | PARTIAL: confirmar resolución de `{{baseUrl}}` tras importar |
| FR-E2E-003 login y copia manual de token | README, preparación/login; requests AUTH-01/02 | PASS | PARTIAL: login de cuatro actores reportado; login por email por confirmar |
| FR-E2E-004 token separado por actor | Plantilla de entorno | PASS: userA/userB/staff/admin separados y sensibles | PASS: USER_A, USER_B, STAFF y ADMIN obtuvieron Bearer; algunos tokens expiraron/renovaron |
| FR-E2E-005 comparar HTTP y JSON a mano | Checklist, README y descripciones de la colección | PASS documental | PARTIAL: inspección de valores JSON y ErrorResponse incompleta |
| FR-E2E-006 encadenar IDs/códigos | README, sección de preparación/orden; variables del entorno | PASS | PASS en flujos ya ejecutados |
| FR-E2E-007 roles, propiedad, 401/403/404 | Checklist, Anexo A #1–22 y casos USER/SEC/EVT/TKT | PASS documental | PARTIAL: STAFF publish y algunos escenarios de token pendientes |
| FR-E2E-008 reconsultar estado después de transición/denegación | README, orden resumido; checklist EVT/TKT | PASS documental | PARTIAL: estados de tickets y publish denegado reconsultados; faltan verificación de asociación duplicada y otros casos |
| FR-E2E-009 ErrorResponse de cinco campos y sin secretos | Checklist ERR-01/02; instrucciones de cierre en README | PASS como criterio escrito | NOT RUN: no hay revisión explícita de todas las formas/códigos |
| FR-E2E-010 fixtures/restauración segura | README y fixtures/README.md | PASS documental: DB dedicada, runId y restauración limitada a pulsepass_e2e | PARTIAL: el reset limpio no se ejecutó en esta sesión |
| FR-E2E-011 pasos operativos completos | `README.md` | PASS | PASS documental; ejecución con pendientes |
| FR-E2E-012 bitácora de caso/actor/esperado/observado/evidencia | `registro-ejecucion-manual.md` | PASS estructura | PARTIAL: reportes manuales sin capturas; filas pendientes explícitas |
| FR-E2E-013 no exportar secretos | Colección/env JSON; README y .gitignore | PASS estático: sin contraseñas/JWT en artefactos y valores sensibles vacíos | PASS para los archivos entregados; valores locales de Postman no exportados |
| FR-E2E-014 registrar defectos/regresiones | Bitácora “Incidentes y regresiones” | PASS estructura | PARTIAL: no hay regresión confirmada; incidentes de configuración anotados, requiere cierre |
| FR-E2E-015 marcar dependencias no satisfechas como BLOCKED | Checklist + README | PASS | PASS: precondiciones faltantes se marcan BLOCKED y los demás casos no ejecutados permanecen NOT RUN |
| FR-E2E-016 repetibilidad/aislamiento | README y fixtures/README.md | PASS documental | PARTIAL: no se repitió desde BD limpia |
| FR-E2E-017 alineación con DTO/código real | Colección, README y matriz del Anexo A | PASS en campos revisados: registro, evento, compra, enums | PARTIAL: importación Postman y contratos visuales de todos los JSON pendientes |

## Criterios AC y DoD

| Criterio | Estado | Base para la decisión |
|---|---|---|
| AC-01 Registro, login y `/me` | PASS nominal | Registro 201 y `/me` confirmó USER_A/B/ADMIN/STAFF; verificar login por email aparte |
| AC-02 ADMIN crea/asocia/publica y público lo ve | PASS | `PP-PUB-r20261018a` quedó PUBLISHED y visible en consultas públicas |
| AC-03 USER no crea/publica, ningún recurso muta | PARTIAL | Crear 403/no existe y publish USER_A 403/DRAFT; publish STAFF pendiente |
| AC-04 USER_B no lee/cancela ticket A | PARTIAL | 403 para lectura/lista/cancelación; falta GET inmediatamente después de cancelación denegada |
| AC-05 Compra cruzada denegada sin ticket nuevo | PARTIAL | 403; falta evidencia de la lista de USER_B inmediatamente posterior |
| AC-06 STAFF usa ticket y segunda transición falla | PASS | 200/USED, 409 en repetición, GET posterior USED |
| AC-07 JWT manipulado/mal formado/vencido | PARTIAL | Mal formado e inválido 401; alterado y token query pendientes; expirado BLOCKED por falta de fixture controlado |
| AC-08 Draft oculto al público y visible a STAFF/ADMIN | PARTIAL | Público denegado/oculta; ADMIN ve; consulta operativa STAFF del draft pendiente |
| AC-09 ErrorResponse | NOT RUN | Faltan inspección explícita de cinco campos en 400/401/403/409 |
| AC-10 22 rutas manuales y evidencia | PARTIAL | 22 rutas base reportadas ejecutadas; hay casos pendientes y capturas no adjuntas |
| DoD global | NO APROBADO | Colección no importada/validada desde UI, escenarios de seguridad/error y evidencia todavía pendientes |



