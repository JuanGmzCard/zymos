-- ============================================================
-- Alera V6: Gestión de proveedores
-- ============================================================

CREATE TABLE IF NOT EXISTS proveedores (
    id          BIGSERIAL    PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL UNIQUE,
    nit         VARCHAR(50),
    telefono    VARCHAR(50),
    email       VARCHAR(100),
    direccion   TEXT,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP
);

-- FK opcional en facturas (nullable para compat. con registros existentes)
ALTER TABLE facturas_proveedor
    ADD COLUMN IF NOT EXISTS proveedor_id BIGINT REFERENCES proveedores(id) ON DELETE SET NULL;
