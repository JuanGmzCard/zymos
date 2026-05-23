-- V16 intentó eliminar constraints únicas simples para hacerlas compuestas con tenant_id,
-- pero las constraints reales tienen nombres generados por JPA/Hibernate (hash).
-- Este script elimina dinámicamente cualquier constraint única de columna simple
-- que haya quedado en las tablas multi-tenant afectadas.

DO $$
DECLARE
    r RECORD;
    tablas TEXT[] := ARRAY['tipos_cerveza','recetas','proveedores','lotes_cerveza'];
    cols   TEXT[] := ARRAY['nombre',       'nombre',  'nombre',     'codigo_lote'];
    i INT;
BEGIN
    FOR i IN 1..array_length(tablas, 1) LOOP
        FOR r IN
            SELECT c.conname
            FROM pg_constraint c
            JOIN pg_class t ON t.oid = c.conrelid
            JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = c.conkey[1]
            WHERE t.relname = tablas[i]
              AND c.contype = 'u'
              AND array_length(c.conkey, 1) = 1
              AND a.attname = cols[i]
        LOOP
            EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I',
                           tablas[i], r.conname);
            RAISE NOTICE 'Eliminada constraint % de tabla %', r.conname, tablas[i];
        END LOOP;
    END LOOP;
END;
$$;

-- Garantizar que las constraints compuestas existan
CREATE UNIQUE INDEX IF NOT EXISTS ux_tipos_cerveza_nombre_tenant  ON tipos_cerveza(nombre, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_recetas_nombre_tenant        ON recetas(nombre, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_proveedores_nombre_tenant    ON proveedores(nombre, tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_lotes_codigo_tenant          ON lotes_cerveza(codigo_lote, tenant_id);
