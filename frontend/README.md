# PulsePass Frontend

Frontend SPA de PulsePass integrado dentro del repositorio Spring Boot, en la carpeta `frontend/`.

## Requisitos

- Node.js **22.22 o superior** (el PRD lo exige para React Router v8).
- npm 10 o superior.
- Backend PulsePass ejecutándose en `http://localhost:8080` y conectado a PostgreSQL.

## Instalación y ejecución

Desde una terminal, en la raíz del repositorio:

```bash
cd frontend
npm install
npm run dev
```

Abre <http://localhost:5173>. Vite reenvía las solicitudes `/api` al backend `http://localhost:8080`.

En otra terminal, desde la raíz del proyecto, inicia Spring Boot según las variables de entorno de tu instalación:

```bash
./mvnw spring-boot:run
```

En Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

El backend requiere la configuración de PostgreSQL y `PULSEPASS_JWT_SECRET_BASE64` que se documentan en el README principal. No se agregaron contraseñas, claves JWT ni credenciales al frontend.

## Comprobaciones

```bash
npm run build
npm test
npm run lint
```

## Arquitectura

- `src/lib/http.ts`: fetch nativo, Bearer JWT, `ApiError` y traducción de errores.
- `src/lib/api.ts`: cliente de las operaciones REST del backend.
- `src/lib/schemas.ts`: esquemas Zod derivados de los DTO Java reales.
- `src/auth/AuthContext.tsx`: sesión, token en `sessionStorage` y roles.
- `src/router.tsx`: rutas con carga diferida y guardas por rol.
- `src/pages/*.tsx`: una página por módulo (export default), cargada con `import()` dinámico desde el router; solo `Inicio` y el layout van en el bundle inicial.
- `src/layouts/`: `Layout`, `RutaProtegida`, `RutaPorRol`, `SoloAnonimo` (guardas como layout routes con `<Outlet />`).
- `src/components/`: UI genérica (`UI.tsx`, `Modal`), `EventCard`, `TicketRow`, `TicketNoDisponible`.
- `src/lib/dominio.ts`: nombres y tonos de estados/categorías/tipos.
- `src/index.css`: tema visual claro/oscuro, tokens de contraste y Tailwind CSS v4.

## Rutas principales

| Área | Rutas |
|---|---|
| Público | `/`, `/eventos`, `/eventos/:eventCode`, `/artistas`, `/artistas/:id`, `/sedes`, `/sedes/:code` |
| Cuenta | `/login`, `/registro`, `/cuenta` |
| USER | `/mis-tickets`, `/mis-tickets/:ticketCode`, `/compra/confirmacion/:ticketCode` |
| STAFF | `/staff/validacion`, `/staff/eventos/:eventCode/tickets` |
| ADMIN | `/admin/eventos`, `/admin/eventos/nuevo`, `/admin/eventos/:eventCode` |

## Consideraciones heredadas de la API

- El registro crea la cuenta pero no inicia sesión automáticamente: se redirige al login.
- Los tickets propios se consultan usando el correo devuelto por `/api/auth/me`.
- El proyecto incorpora `GET /api/events/admin` para que ADMIN liste eventos y borradores sin depender solo de códigos guardados en la sesión.
- El precio de las entradas lo calcula el backend; el frontend no envía un precio.
- El token dura 15 minutos y no existe refresh token ni logout remoto. Al expirar se limpia la sesión local.
- El frontend no implementa pasarela de pagos ni generación de QR, porque están fuera del alcance del PRD.

## Temas visuales y referencias del PRD

- El selector **Modo claro / Modo oscuro** está en la barra superior y en el menú móvil. La elección se conserva en el navegador; si no hay una elección guardada, se toma inicialmente la preferencia del sistema.
- Los estilos de superficies, texto, campos, bordes y estados usan tokens CSS para conservar contraste en ambos temas.
- Las 32 imágenes de referencia de la sección 9.0 del PRD están en `../docs/diseno/`.
- La auditoría de requisitos, cambios y pendientes está en `../docs/AUDITORIA_PRD_PULSEPASS.md`.

## Pruebas

`npm test` ejecuta Vitest + Testing Library. La API se simula a nivel de `fetch` con una tabla de rutas (`src/test/mockApi.ts`), sin red real ni reloj real. La aplicación real (rutas, guardas, layout, AuthProvider) se monta con `renderApp()` (`src/test/utils.tsx`).

| Archivo | Cubre |
|---|---|
| `http.test.ts`, `schemas.test.ts`, `formato.test.ts`, `rutas.test.ts` | Cliente HTTP, mapeo de errores, contratos zod, formatos, rutas |
| `guardas.test.tsx` | Anónimo/USER/STAFF/ADMIN, 403, sesión expirada, 401 del servidor, navbar por rol, 404, evento no disponible |
| `autenticacion.test.tsx` | Login (redirección, Bearer, sessionStorage), 401 genérico, logout, registro 201/409/400 |
| `tickets.test.tsx` | Compra (email de sesión, 409 en modal, anónimo, agotado, STAFF), mis tickets, cancelar, ticket ajeno |
| `operacion-admin.test.tsx` | Validación de ingreso (éxito/409/404), tickets pagados, crear/publicar evento, asociar artista |
| `forms.test.tsx`, `UI.test.tsx` | Validación previa a la API y componentes accesibles |

Pendiente respecto al PRD: sustituir el mock de `fetch` por **MSW** (requiere `npm install -D msw` y actualizar `package-lock.json` con red) y cubrir los casos restantes de la sección 21 (artistas, sedes, filtros de cartelera en la URL, menú móvil por teclado).

## Rutas administrativas adicionales

Además de las rutas del PRD, se conserva `/admin/catalogos` para dar de alta sedes y artistas, junto con el listado administrativo de eventos y borradores. Estas son ampliaciones del proyecto que ya estaban implementadas; la auditoría distingue esas ampliaciones de los requisitos originales.
