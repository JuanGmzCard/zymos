-- Índices para columnas de filtrado frecuente no cubiertas por V34

-- equipos.estado — EquipoRepository filtra por estado en lista y listarPorEstado
CREATE INDEX IF NOT EXISTS idx_equipos_tenant_estado
    ON equipos(tenant_id, estado);

-- equipos.tipo — filtro por tipo en lista
CREATE INDEX IF NOT EXISTS idx_equipos_tenant_tipo
    ON equipos(tenant_id, tipo);

-- insumos_inventario.tipo — findByFiltros filtra por tipo; detectarTipo en service
CREATE INDEX IF NOT EXISTS idx_insumos_tenant_tipo
    ON insumos_inventario(tenant_id, tipo);

-- facturas_proveedor.proveedor — typeahead search y agrupación por proveedor en Excel
CREATE INDEX IF NOT EXISTS idx_facturas_proveedor_nombre
    ON facturas_proveedor(tenant_id, LOWER(proveedor));
