-- V16 intentó eliminar la constraint única simple de username en usuarios,
-- pero la constraint real tiene nombre generado por JPA/Hibernate.
-- Este script la elimina explícitamente y garantiza la constraint compuesta.

ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS uk_m2dvbwfge291euvmk6vkkocao;
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_username_key;

CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_username_tenant
    ON usuarios(username, tenant_id);
