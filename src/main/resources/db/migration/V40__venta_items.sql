-- V40: Convertir ventas a modelo multi-ítem (multi-lote por venta)

CREATE TABLE IF NOT EXISTS venta_items (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(100)  NOT NULL,
    venta_id        BIGINT        NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    lote_id         BIGINT        REFERENCES lotes_cerveza(id) ON DELETE SET NULL,
    codigo_lote     VARCHAR(50),
    descripcion     VARCHAR(200),
    cantidad        DECIMAL(10,3) NOT NULL CHECK (cantidad > 0),
    unidad          VARCHAR(50),
    precio_unitario DECIMAL(12,2) NOT NULL,
    descuento_pct   DECIMAL(5,2)  NOT NULL DEFAULT 0
                    CHECK (descuento_pct BETWEEN 0 AND 100)
);

CREATE INDEX IF NOT EXISTS idx_venta_items_venta  ON venta_items (venta_id);
CREATE INDEX IF NOT EXISTS idx_venta_items_tenant ON venta_items (tenant_id);
CREATE INDEX IF NOT EXISTS idx_venta_items_lote   ON venta_items (lote_id) WHERE lote_id IS NOT NULL;

-- Migrar registros existentes: cada venta actual pasa a tener un ítem
INSERT INTO venta_items
    (tenant_id, venta_id, lote_id, codigo_lote, cantidad, unidad, precio_unitario, descuento_pct)
SELECT
    tenant_id, id, lote_id, codigo_lote, cantidad, unidad, precio_unitario, COALESCE(descuento_pct, 0)
FROM ventas
WHERE deleted_at IS NULL
  AND cantidad IS NOT NULL
  AND precio_unitario IS NOT NULL;

-- Eliminar columnas que se mueven a venta_items
ALTER TABLE ventas DROP COLUMN IF EXISTS lote_id;
ALTER TABLE ventas DROP COLUMN IF EXISTS codigo_lote;
ALTER TABLE ventas DROP COLUMN IF EXISTS cantidad;
ALTER TABLE ventas DROP COLUMN IF EXISTS unidad;
ALTER TABLE ventas DROP COLUMN IF EXISTS precio_unitario;
ALTER TABLE ventas DROP COLUMN IF EXISTS descuento_pct;
