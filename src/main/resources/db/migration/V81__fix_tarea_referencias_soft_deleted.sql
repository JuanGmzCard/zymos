-- V81: Elimina referencias de tareas que apuntan a registros con soft-delete
-- (deleted_at IS NOT NULL) cuya migración en V79 no filtró correctamente.
-- Afecta lotes_cerveza y recetas, que usan @SQLRestriction(deleted_at IS NULL).

DELETE FROM tarea_referencias tr
WHERE tr.tipo = 'LOTE'
  AND NOT EXISTS (
      SELECT 1 FROM lotes_cerveza l
      WHERE l.id = tr.entidad_id
        AND l.tenant_id = tr.tenant_id
        AND l.deleted_at IS NULL
  );

DELETE FROM tarea_referencias tr
WHERE tr.tipo = 'RECETA'
  AND NOT EXISTS (
      SELECT 1 FROM recetas r
      WHERE r.id = tr.entidad_id
        AND r.tenant_id = tr.tenant_id
        AND r.deleted_at IS NULL
  );
