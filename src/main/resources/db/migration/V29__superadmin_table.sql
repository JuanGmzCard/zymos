-- Tabla de super-administradores globales (sin tenant_id — acceso a todos los tenants)
CREATE TABLE super_admins (
    id       BIGSERIAL    PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    activo   BOOLEAN      NOT NULL DEFAULT TRUE
);
