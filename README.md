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