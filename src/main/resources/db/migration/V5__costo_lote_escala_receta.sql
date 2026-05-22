-- ============================================================
-- Alera V5: Control de costo por lote + escala de receta
-- ============================================================

-- Volumen base en recetas para la calculadora de escala
ALTER TABLE recetas
    ADD COLUMN IF NOT EXISTS volumen_base DECIMAL(10,3);

-- Relación N:M entre lotes y facturas (costo de producción)
CREATE TABLE IF NOT EXISTS lote_facturas (
    lote_id    BIGINT NOT NULL REFERENCES lotes_cerveza(id)     ON DELETE CASCADE,
    factura_id BIGINT NOT NULL REFERENCES facturas_proveedor(id) ON DELETE CASCADE,
    PRIMARY KEY (lote_id, factura_id)
);
