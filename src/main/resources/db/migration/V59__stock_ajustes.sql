-- Ajustes manuales de stock de producto terminado
-- Registra mermas, muestras, degustaciones y correcciones sobre los lotes completados.
-- El stock disponible = litros_finales - sum(venta_items) + sum(stock_ajustes.cantidad)
-- cantidad positiva = entrada/corrección, cantidad negativa = merma/pérdida

CREATE TABLE stock_ajustes (
    id         BIGSERIAL    PRIMARY KEY,
    tenant_id  VARCHAR(100) NOT NULL,
    lote_id    BIGINT       NOT NULL REFERENCES lotes_cerveza(id),
    cantidad   DECIMAL(10, 3) NOT NULL,
    unidad     VARCHAR(50)  NOT NULL DEFAULT 'L',
    motivo     VARCHAR(500) NOT NULL,
    fecha      DATE         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100)
);

CREATE INDEX idx_stock_ajustes_lote   ON stock_ajustes(lote_id, tenant_id);
CREATE INDEX idx_stock_ajustes_tenant ON stock_ajustes(tenant_id);
