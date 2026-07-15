-- Reemplaza el índice único global por uno parcial que excluye recetas archivadas,
-- permitiendo reutilizar el nombre de una receta que fue archivada (deleted_at IS NOT NULL).
DROP INDEX IF EXISTS ux_recetas_nombre_tenant;

CREATE UNIQUE INDEX ux_recetas_nombre_tenant
    ON recetas(nombre, tenant_id)
    WHERE deleted_at IS NULL;
