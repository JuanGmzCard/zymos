-- Índices compuestos para lotes_cerveza (dashboard, lista paginada, reportes)
-- Todos parciales WHERE deleted_at IS NULL — excluyen lotes archivados del B-tree.

-- (tenant_id, fecha_elaboracion): cubre findLitrosPorMes (chart dashboard) y
-- findByFiltros con filtro de rango de fechas.
CREATE INDEX IF NOT EXISTS idx_lotes_tenant_fecha
    ON lotes_cerveza(tenant_id, fecha_elaboracion)
    WHERE deleted_at IS NULL;

-- (tenant_id, created_at DESC): cubre findByFiltros ORDER BY, findTop5 y
-- findAllCompletados — la columna de orden más usada en la lista principal.
CREATE INDEX IF NOT EXISTS idx_lotes_tenant_created
    ON lotes_cerveza(tenant_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- (tenant_id, estilo): cubre findLotesPorEstilo (chart top-6 estilos) y
-- el filtro LIKE LOWER(estilo) en findByFiltros.
CREATE INDEX IF NOT EXISTS idx_lotes_tenant_estilo
    ON lotes_cerveza(tenant_id, LOWER(estilo))
    WHERE deleted_at IS NULL;
