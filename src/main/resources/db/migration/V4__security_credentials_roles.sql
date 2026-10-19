-- Fase 4: credenciales y roles. No altera tablas existentes ni asigna contrasenas.
-- Los usuarios previos quedan SIN credenciales (no pueden iniciar sesion) hasta una activacion segura.
CREATE TABLE user_credentials (
    user_id BIGINT PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL,
    password_changed_at TIMESTAMP,
    CONSTRAINT fk_credentials_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_role CHECK (role IN ('USER', 'STAFF', 'ADMIN')),
    CONSTRAINT fk_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Los usuarios existentes conservan unicamente el rol minimo; sin credenciales no pueden autenticarse.
INSERT INTO user_roles (user_id, role) SELECT id, 'USER' FROM users;
