CREATE TABLE factura_historial_estado (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL DEFAULT 'default',
    factura_id      BIGINT       NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20)  NOT NULL,
    usuario         VARCHAR(100),
    fecha           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fact_hist_est_factura ON factura_historial_estado(factura_id);
CREATE INDEX idx_fact_hist_est_tenant  ON factura_historial_estado(tenant_id);
