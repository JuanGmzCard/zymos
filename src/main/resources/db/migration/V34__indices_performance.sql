-- Índices para columnas de filtrado frecuente
-- fecha_factura — usada en findAllFiltered, findSinProcesar, sumTotalFiltered
CREATE INDEX IF NOT EXISTS idx_facturas_fecha
    ON facturas_proveedor(fecha_factura DESC, tenant_id);

-- estado — usada en findAllFiltered, findSinProcesar, sumPorEstados, countPorEstados
CREATE INDEX IF NOT EXISTS idx_facturas_estado
    ON facturas_proveedor(estado, tenant_id);

-- deleted_at — @SQLRestriction("deleted_at IS NULL") se aplica en cada query de lotes
CREATE INDEX IF NOT EXISTS idx_lotes_deleted
    ON lotes_cerveza(deleted_at) WHERE deleted_at IS NOT NULL;

-- fecha_vencimiento — usada en findProximosAVencer
CREATE INDEX IF NOT EXISTS idx_insumos_vencimiento
    ON insumos_inventario(fecha_vencimiento, tenant_id) WHERE fecha_vencimiento IS NOT NULL;
