-- =============================================================================
-- db_security.sql — Principio de mínimo privilegio en PostgreSQL
-- =============================================================================
-- Ejecutar UNA VEZ como superusuario en el servidor de producción:
--   psql -U postgres -d trazabilidad_cervezas -f db_security.sql
--
-- Luego cambiar las contraseñas placeholder:
--   psql -U postgres -c "\password zymos_app"
--   psql -U postgres -c "\password zymos_flyway"
--
-- Roles que crea este script:
--   zymos_app    — solo DML (SELECT/INSERT/UPDATE/DELETE). Usado por HikariCP.
--   zymos_flyway — DDL completo. Usado solo por Flyway en cada deploy.
--
-- Variables de entorno resultantes (configurar en .env de producción):
--   DB_USERNAME=zymos_app        DB_PASSWORD=<contraseña_fuerte>
--   FLYWAY_USERNAME=zymos_flyway FLYWAY_PASSWORD=<contraseña_flyway>
-- =============================================================================

\set ON_ERROR_STOP on

-- ── 1. Crear roles (idempotente) ──────────────────────────────────────────────
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'zymos_app') THEN
    CREATE ROLE zymos_app LOGIN PASSWORD 'CAMBIAR_ESTA_CONTRASENA';
    RAISE NOTICE 'Rol zymos_app creado. Cambiar contraseña con: \password zymos_app';
  ELSE
    RAISE NOTICE 'Rol zymos_app ya existe — sin cambios en el rol.';
  END IF;

  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'zymos_flyway') THEN
    CREATE ROLE zymos_flyway LOGIN PASSWORD 'CAMBIAR_ESTA_CONTRASENA_FLYWAY';
    RAISE NOTICE 'Rol zymos_flyway creado. Cambiar contraseña con: \password zymos_flyway';
  ELSE
    RAISE NOTICE 'Rol zymos_flyway ya existe — sin cambios en el rol.';
  END IF;
END
$$;

-- ── 2. Acceso a la base de datos ─────────────────────────────────────────────
GRANT CONNECT ON DATABASE trazabilidad_cervezas TO zymos_app;
GRANT CONNECT ON DATABASE trazabilidad_cervezas TO zymos_flyway;

-- ── 3. Schema public ─────────────────────────────────────────────────────────
-- zymos_app: solo puede usar el schema (no crear objetos)
GRANT USAGE ON SCHEMA public TO zymos_app;
-- zymos_flyway: puede crear tablas/índices/secuencias (necesario para migraciones)
GRANT USAGE, CREATE ON SCHEMA public TO zymos_flyway;

-- ── 4. Tablas y secuencias YA EXISTENTES ─────────────────────────────────────
-- zymos_app: DML solamente — no puede ALTER TABLE, DROP, ni TRUNCATE
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO zymos_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO zymos_app;

-- zymos_flyway: DDL completo (ALTER TABLE, DROP, CREATE INDEX, etc.)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO zymos_flyway;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO zymos_flyway;

-- ── 5. Permisos por defecto para TABLAS FUTURAS ───────────────────────────────
-- Cada vez que zymos_flyway cree una tabla en una nueva migración,
-- zymos_app recibe automáticamente DML sin necesidad de GRANT manual.
ALTER DEFAULT PRIVILEGES FOR ROLE zymos_flyway IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO zymos_app;

ALTER DEFAULT PRIVILEGES FOR ROLE zymos_flyway IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO zymos_app;

-- ── 6. Verificación ───────────────────────────────────────────────────────────
\echo ''
\echo '============================================================'
\echo 'Roles y permisos configurados correctamente.'
\echo ''
\echo 'IMPORTANTE — cambiar contraseñas antes de ir a producción:'
\echo '  psql -U postgres -c "\password zymos_app"'
\echo '  psql -U postgres -c "\password zymos_flyway"'
\echo ''
\echo 'Variables de entorno para .env:'
\echo '  DB_USERNAME=zymos_app'
\echo '  DB_PASSWORD=<contraseña_zymos_app>'
\echo '  FLYWAY_USERNAME=zymos_flyway'
\echo '  FLYWAY_PASSWORD=<contraseña_zymos_flyway>'
\echo '============================================================'
\echo ''
