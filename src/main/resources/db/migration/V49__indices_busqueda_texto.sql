-- Índices compuestos para búsquedas de texto por tenant
-- Las búsquedas con LIKE LOWER() requieren (tenant_id, LOWER(campo)) para evitar seq scan

-- recetas.nombre — suggest, search, filtros de lista
CREATE INDEX IF NOT EXISTS idx_recetas_tenant_nombre
    ON recetas(tenant_id, LOWER(nombre))
    WHERE deleted_at IS NULL;

-- proveedores.nombre — suggest, filtro de lista y typeahead en facturas
CREATE INDEX IF NOT EXISTS idx_proveedores_tenant_nombre
    ON proveedores(tenant_id, LOWER(nombre));

-- proveedores.nit — suggest también filtra por NIT
CREATE INDEX IF NOT EXISTS idx_proveedores_tenant_nit
    ON proveedores(tenant_id, LOWER(COALESCE(nit, '')))
    WHERE nit IS NOT NULL;

-- insumos_inventario.nombre — reemplaza idx_insumos_nombre (solo LOWER(nombre), sin tenant)
-- El índice existente sigue siendo válido pero incompleto para multi-tenant;
-- este índice compuesto cubre consultas filtradas por tenant
CREATE INDEX IF NOT EXISTS idx_insumos_tenant_nombre
    ON insumos_inventario(tenant_id, LOWER(nombre));
