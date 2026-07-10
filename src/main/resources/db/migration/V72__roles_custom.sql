-- Roles personalizados por tenant
CREATE TABLE roles_tenant (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    es_sistema  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, nombre)
);

-- Permisos granulares por módulo
CREATE TABLE roles_modulos_permisos (
    id              BIGSERIAL PRIMARY KEY,
    rol_id          BIGINT NOT NULL REFERENCES roles_tenant(id) ON DELETE CASCADE,
    modulo          VARCHAR(50) NOT NULL,
    puede_ver       BOOLEAN NOT NULL DEFAULT FALSE,
    puede_crear     BOOLEAN NOT NULL DEFAULT FALSE,
    puede_editar    BOOLEAN NOT NULL DEFAULT FALSE,
    puede_eliminar  BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (rol_id, modulo)
);

-- FK en usuarios para rol custom (nullable — si NULL usa el enum rol)
ALTER TABLE usuarios ADD COLUMN rol_custom_id BIGINT REFERENCES roles_tenant(id) ON DELETE SET NULL;

CREATE INDEX idx_roles_tenant_tenant_id   ON roles_tenant(tenant_id);
CREATE INDEX idx_roles_permisos_rol_id    ON roles_modulos_permisos(rol_id);
CREATE INDEX idx_usuarios_rol_custom_id   ON usuarios(rol_custom_id);
