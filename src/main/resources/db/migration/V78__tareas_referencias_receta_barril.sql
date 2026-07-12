-- V78: Referencias de tareas a Receta y Barril (prioridad baja)
ALTER TABLE tareas
    ADD COLUMN receta_id BIGINT REFERENCES recetas(id)  ON DELETE SET NULL,
    ADD COLUMN barril_id BIGINT REFERENCES barriles(id) ON DELETE SET NULL;

CREATE INDEX idx_tareas_receta ON tareas(tenant_id, receta_id) WHERE receta_id IS NOT NULL;
CREATE INDEX idx_tareas_barril ON tareas(tenant_id, barril_id) WHERE barril_id IS NOT NULL;
