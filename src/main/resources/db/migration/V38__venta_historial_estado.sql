-- Historial de cambios de estado de ventas
CREATE TABLE IF NOT EXISTS venta_historial_estado (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    venta_id        BIGINT NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20) NOT NULL,
    usuario         VARCHAR(100),
    fecha           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_venta_hist_venta  ON venta_historial_estado (venta_id);
CREATE INDEX IF NOT EXISTS idx_venta_hist_tenant ON venta_historial_estado (tenant_id);