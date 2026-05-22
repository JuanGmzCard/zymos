-- Drop unused join table (feature was never exposed in UI; no data)
DROP TABLE IF EXISTS lote_facturas;

-- Junction table: partial assignment of invoice items to batches
-- cantidadAsignada allows splitting one item across multiple batches
CREATE TABLE lote_items_factura (
    id                BIGSERIAL     PRIMARY KEY,
    lote_id           BIGINT        NOT NULL REFERENCES lotes_cerveza(id)  ON DELETE CASCADE,
    factura_item_id   BIGINT        NOT NULL REFERENCES factura_items(id)  ON DELETE CASCADE,
    cantidad_asignada DECIMAL(10,3) NOT NULL CHECK (cantidad_asignada > 0),
    UNIQUE (lote_id, factura_item_id)
);

CREATE INDEX idx_lote_items_factura_lote ON lote_items_factura(lote_id);
CREATE INDEX idx_lote_items_factura_item ON lote_items_factura(factura_item_id);