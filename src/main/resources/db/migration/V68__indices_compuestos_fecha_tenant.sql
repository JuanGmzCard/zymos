-- V68: Índices compuestos (tenant_id + fecha) para queries de reportes y dashboard.
-- Los índices simples de tenant_id en V16 no ayudan en filtros por rango de fecha.

CREATE INDEX IF NOT EXISTS idx_lotes_tenant_fecha
    ON lotes_cerveza(tenant_id, fecha_elaboracion);

CREATE INDEX IF NOT EXISTS idx_log_tenant_fecha
    ON log_accesos(tenant_id, fecha DESC);

CREATE INDEX IF NOT EXISTS idx_facturas_tenant_fecha
    ON facturas_proveedor(tenant_id, fecha_factura);
