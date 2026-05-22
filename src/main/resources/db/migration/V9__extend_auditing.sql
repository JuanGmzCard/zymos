-- ============================================================
-- Alera V9: Auditoría completa en entidades clave
-- created_at ya existe en: lotes_cerveza, recetas, equipos, proveedores
-- last_modified ya existe en: lotes_cerveza (V7)
-- ============================================================

-- lotes_cerveza: solo falta created_by
ALTER TABLE lotes_cerveza
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);

-- recetas: created_at existe, faltan el resto
ALTER TABLE recetas
    ADD COLUMN IF NOT EXISTS created_by       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_modified_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by  VARCHAR(100);

-- equipos: created_at existe (V1), faltan el resto
ALTER TABLE equipos
    ADD COLUMN IF NOT EXISTS created_by       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_modified_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by  VARCHAR(100);

-- insumos_inventario: sin auditoría, agregar los 4
ALTER TABLE insumos_inventario
    ADD COLUMN IF NOT EXISTS created_at        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_modified_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by  VARCHAR(100);

-- facturas_proveedor: sin auditoría, agregar los 4
ALTER TABLE facturas_proveedor
    ADD COLUMN IF NOT EXISTS created_at        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_modified_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by  VARCHAR(100);

-- proveedores: created_at existe (V6), faltan el resto
ALTER TABLE proveedores
    ADD COLUMN IF NOT EXISTS created_by       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_modified_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by  VARCHAR(100);
