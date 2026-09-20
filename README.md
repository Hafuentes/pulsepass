# PulsePass — Plataforma de Eventos, Artistas y Entradas

Caso de estudio académico para la capa de persistencia con Java 21, Spring Boot 4, JPA, PostgreSQL y Flyway.

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

## Ejecución de Pruebas de Integración
Para ejecutar todas las pruebas contra una instancia real de PostgreSQL gestionada por Testcontainers:

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

