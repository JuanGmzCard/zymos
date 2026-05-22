-- Agregar tenant_id a todas las tablas de datos.
-- Las filas existentes reciben 'default' (el tenant inicial de la instalación).

ALTER TABLE lotes_cerveza         ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE ingredientes           ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE recetas                ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE receta_ingredientes    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE escalones_macerado     ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE adiciones_hervor       ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE historial_lotes        ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE log_accesos            ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE equipos                ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE mantenimientos_equipo  ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE insumos_inventario     ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE facturas_proveedor     ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE factura_items          ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE proveedores            ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE tipos_cerveza          ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE usuarios               ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE lote_items_factura     ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';

-- Índices para filtrado eficiente por tenant
CREATE INDEX IF NOT EXISTS idx_lotes_tenant          ON lotes_cerveza(tenant_id);
CREATE INDEX IF NOT EXISTS idx_recetas_tenant        ON recetas(tenant_id);
CREATE INDEX IF NOT EXISTS idx_equipos_tenant        ON equipos(tenant_id);
CREATE INDEX IF NOT EXISTS idx_insumos_tenant        ON insumos_inventario(tenant_id);
CREATE INDEX IF NOT EXISTS idx_facturas_tenant       ON facturas_proveedor(tenant_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_tenant       ON usuarios(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tipos_tenant          ON tipos_cerveza(tenant_id);
CREATE INDEX IF NOT EXISTS idx_proveedores_tenant    ON proveedores(tenant_id);

-- Unicidad tenant-scoped: reemplazar constraints de columna única
-- por constraints compuestos (valor + tenant_id)

ALTER TABLE lotes_cerveza  DROP CONSTRAINT IF EXISTS lotes_cerveza_codigo_lote_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_lotes_codigo_tenant
    ON lotes_cerveza(codigo_lote, tenant_id);

ALTER TABLE recetas         DROP CONSTRAINT IF EXISTS recetas_nombre_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_recetas_nombre_tenant
    ON recetas(nombre, tenant_id);

ALTER TABLE proveedores     DROP CONSTRAINT IF EXISTS proveedores_nombre_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_proveedores_nombre_tenant
    ON proveedores(nombre, tenant_id);

ALTER TABLE tipos_cerveza   DROP CONSTRAINT IF EXISTS tipos_cerveza_nombre_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_tipos_cerveza_nombre_tenant
    ON tipos_cerveza(nombre, tenant_id);

ALTER TABLE usuarios        DROP CONSTRAINT IF EXISTS usuarios_username_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_username_tenant
    ON usuarios(username, tenant_id);
