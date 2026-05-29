-- V37: Módulo de Ventas / Despacho
CREATE TABLE ventas (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         VARCHAR(100) NOT NULL,
    lote_id           BIGINT       REFERENCES lotes_cerveza(id) ON DELETE SET NULL,
    codigo_lote       VARCHAR(50),
    cliente           VARCHAR(200) NOT NULL,
    fecha_despacho    DATE         NOT NULL,
    cantidad          DECIMAL(10,3) NOT NULL,
    unidad            VARCHAR(50),
    precio_unitario   DECIMAL(12,2) NOT NULL,
    descuento_pct     DECIMAL(5,2)  NOT NULL DEFAULT 0,
    notas             VARCHAR(500),
    estado            VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100),
    last_modified_at  TIMESTAMP,
    last_modified_by  VARCHAR(100),
    CONSTRAINT chk_ventas_estado CHECK (estado IN ('PENDIENTE','DESPACHADO','CANCELADO')),
    CONSTRAINT chk_ventas_descuento CHECK (descuento_pct >= 0 AND descuento_pct <= 100)
);

CREATE INDEX idx_ventas_tenant       ON ventas(tenant_id);
CREATE INDEX idx_ventas_fecha        ON ventas(tenant_id, fecha_despacho DESC);
CREATE INDEX idx_ventas_lote         ON ventas(lote_id);
CREATE INDEX idx_ventas_estado       ON ventas(tenant_id, estado);
CREATE INDEX idx_ventas_cliente      ON ventas(tenant_id, LOWER(cliente));
