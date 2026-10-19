# PulsePass — pruebas E2E manuales

Este directorio contiene los entregables de pruebas manuales del PRD. Ejecuta cada solicitud desde Postman Desktop y contrasta el HTTP, el JSON y el estado persistido. No uses Collection Runner, Newman ni scripts de colección.

## Estructura de entrega

tests/e2e/
- README.md
- postman/
  - PulsePass-Manual-E2E.postman_collection.json
  - PulsePass-local.postman_environment.json
- fixtures/
  - README.md
  - seed-e2e.sql (opcional; exclusivo de pulsepass_e2e)
- checklist-e2e-manual.md
- registro-ejecucion-manual.md
- .gitignore

La matriz de rutas/roles y la trazabilidad de requisitos se incluyen como anexos de la checklist para mantener el paquete en esta estructura.

## Preparación de Postman

1. Inicia PostgreSQL y la API local. La corrida documentada usó la base dedicada pulsepass_e2e y http://localhost:8080.
2. Importa la colección y la plantilla de entorno desde postman/. Importa la plantilla, no un entorno exportado que contenga valores locales.
3. Selecciona el entorno y verifica la variable baseUrl, escrita exactamente así, con valor http://localhost:8080. Envía la consulta pública de venues activos y confirma 200 antes de iniciar los casos.
4. Completa credenciales solo en valores locales de Postman. Inicia sesión por actor y copia cada access token al valor local sensible correspondiente: userAToken, userBToken, staffToken o adminToken.
5. Envía las solicitudes una por una con Send. La colección no usa scripts ni automatización de ejecución.

La colección contiene 22 solicitudes, parametrizadas con {{baseUrl}} y organizadas por módulos. Todas envían Accept: application/json; las solicitudes con JSON incluyen Content-Type: application/json.

## Datos, actores y repetición

La bitácora conserva la corrida r20261018a: venue VEN-E2E-R20261018A, artista ID 3, evento borrador PP-DRAFT-r20261018a, evento publicado PP-PUB-r20261018a, USER_A ID 1, USER_B ID 2, ADMIN ID 3, STAFF ID 4, ticket ID 1 CANCELLED y ticket ID 2 USED. Son datos locales de esa ejecución; crea usernames, correos, códigos y runId nuevos para repetirla.

No registres roles privilegiados por HTTP ni aceptes role=ADMIN enviado en el registro. El proyecto incluye un perfil local-seed que aprovisiona cuentas de desarrollo únicamente con contraseñas tomadas del entorno; nunca lo actives en producción. Consulta fixtures/README.md. No guardes ni compartas passwords, JWT, hashes o encabezados Authorization.

Para una ejecución limpia, restaura únicamente una copia de la base dedicada pulsepass_e2e y confirma su nombre antes de cualquier operación. No borres ni restaures otras bases. El seed SQL opcional se limita a una sede de prueba y falla si la base seleccionada no se llama exactamente pulsepass_e2e.

## Orden resumido de ejecución

1. Consulta venues y artistas públicos.
2. Registra USER_A y USER_B, valida 400/409 y crea o prepara actores administrativos solo para el entorno local.
3. Inicia sesión por username y email; valida login, roles y /api/auth/me.
4. Comprueba acceso propio/ajeno a usuarios, usando USER_A, USER_B y ADMIN.
5. Crea un evento DRAFT como ADMIN; revisa denegaciones de usuario/anónimo, asociación de artista, publicación y visibilidad pública/operativa.
6. Compra tickets separados para cancelar y usar; valida propiedad, denegación cruzada, consulta posterior, estados CANCELLED/USED y conflictos 409.
7. Ejecuta los casos JWT y errores, revisa los cinco campos de ErrorResponse y registra cada resultado en checklist-e2e-manual.md y registro-ejecucion-manual.md.

Sigue el orden de la sección anterior y consulta la matriz de roles incluida como anexo en la checklist. Cuando una precondición impida ejecutar un caso, registra BLOCKED y explica cuál falta; no lo marques PASS.

## Revisión actual

Los artefactos fueron revisados estáticamente: colección JSON parseable con 22 requests, URLs parametrizadas, encabezados esperados y sin scripts; plantilla JSON parseable, secretos vacíos. Los archivos se ajustaron a la estructura de entrega indicada. La importación en Postman Desktop no se confirmó visualmente en esta sesión.

La ejecución E2E del PRD permanece PARCIAL / NO APROBADA: hay escenarios NOT RUN o BLOCKED y faltan inspecciones explícitas de ErrorResponse y evidencia visual sin secretos. Consulta el estado individual en checklist-e2e-manual.md; no presentes la aprobación documental como aprobación total de pruebas.

