package com.pulse.pass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * DoD Fase 4: Flyway funciona sobre esquema NUEVO y sobre un esquema YA MIGRADO (V1-V3 con datos), y V4
 * respeta las restricciones de roles/credenciales. Cada prueba usa su propio esquema de PostgreSQL.
 */
@Testcontainers
class FlywayMigrationIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));

    private Flyway flyway(String schema, String target) {
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .schemas(schema);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private long count(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    @Test
    void v4_onAlreadyMigratedSchema_keepsLegacyUsersWithUserRoleAndNoCredentials() throws Exception {
        flyway("mig_legacy", "3").migrate(); // estado previo a la Fase 4
        execute("INSERT INTO mig_legacy.users (username, email, active) VALUES ('old-user', 'old@mail.com', true)");

        flyway("mig_legacy", null).migrate(); // aplica V4 sin tocar V1-V3

        Flyway migrated = flyway("mig_legacy", null);
        assertThat(migrated.info().pending()).isEmpty();
        assertThat(migrated.info().current().getVersion().isNewerThan("3")).isTrue();
        assertThat(count("SELECT COUNT(*) FROM mig_legacy.users WHERE username = 'old-user'")).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(*) FROM mig_legacy.user_roles r JOIN mig_legacy.users u ON u.id = r.user_id
                WHERE u.username = 'old-user' AND r.role = 'USER'
                """)).isEqualTo(1);
        // Sin credenciales: no puede iniciar sesion hasta una activacion segura (sin contrasena por defecto).
        assertThat(count("SELECT COUNT(*) FROM mig_legacy.user_credentials")).isZero();
    }

    @Test
    void v4_onFreshSchema_isIdempotent_andEnforcesRoleAndCredentialConstraints() throws Exception {
        Flyway fresh = flyway("mig_fresh", null);
        fresh.migrate();
        fresh.migrate(); // volver a migrar es idempotente: no falla y no deja nada pendiente
        assertThat(fresh.info().pending()).isEmpty();

        execute("INSERT INTO mig_fresh.users (username, email, active) VALUES ('u1', 'u1@mail.com', true)");
        long userId = count("SELECT id FROM mig_fresh.users WHERE username = 'u1'");
        execute("INSERT INTO mig_fresh.user_roles (user_id, role) VALUES (%d, 'USER')".formatted(userId));

        // PK compuesta (user_id, role): no hay roles duplicados
        assertThatThrownBy(() -> execute(
                "INSERT INTO mig_fresh.user_roles (user_id, role) VALUES (%d, 'USER')".formatted(userId)))
                .isInstanceOf(SQLException.class);
        // El rol esta restringido a USER | STAFF | ADMIN
        assertThatThrownBy(() -> execute(
                "INSERT INTO mig_fresh.user_roles (user_id, role) VALUES (%d, 'SUPERUSER')".formatted(userId)))
                .isInstanceOf(SQLException.class);
        // El hash es obligatorio y la relacion es 1:1 con el usuario
        assertThatThrownBy(() -> execute(
                "INSERT INTO mig_fresh.user_credentials (user_id, password_hash) VALUES (%d, NULL)".formatted(userId)))
                .isInstanceOf(SQLException.class);
        execute("INSERT INTO mig_fresh.user_credentials (user_id, password_hash) VALUES (%d, '$2a$10$x')".formatted(userId));
        assertThatThrownBy(() -> execute(
                "INSERT INTO mig_fresh.user_credentials (user_id, password_hash) VALUES (%d, '$2a$10$y')".formatted(userId)))
                .isInstanceOf(SQLException.class);
        // Un rol no puede apuntar a un usuario inexistente
        assertThatThrownBy(() -> execute("INSERT INTO mig_fresh.user_roles (user_id, role) VALUES (999999, 'ADMIN')"))
                .isInstanceOf(SQLException.class);

        // Borrar el usuario elimina credencial y roles (ON DELETE CASCADE)
        execute("DELETE FROM mig_fresh.users WHERE id = %d".formatted(userId));
        assertThat(count("SELECT COUNT(*) FROM mig_fresh.user_roles WHERE user_id = %d".formatted(userId))).isZero();
        assertThat(count("SELECT COUNT(*) FROM mig_fresh.user_credentials WHERE user_id = %d".formatted(userId))).isZero();
    }
}
