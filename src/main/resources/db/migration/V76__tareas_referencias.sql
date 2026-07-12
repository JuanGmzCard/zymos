-- V76: Ampliar referencias de tareas — Insumos, Elaboraciones, Órdenes de compra, Ventas

ALTER TABLE tareas
    ADD COLUMN insumo_id       BIGINT REFERENCES insumos_inventario(id) ON DELETE SET NULL,
    ADD COLUMN elaboracion_id  BIGINT REFERENCES elaboraciones_planificadas(id) ON DELETE SET NULL,
    ADD COLUMN orden_compra_id BIGINT REFERENCES ordenes_compra(id) ON DELETE SET NULL,
    ADD COLUMN venta_id        BIGINT REFERENCES ventas(id) ON DELETE SET NULL;

CREATE INDEX idx_tareas_insumo       ON tareas(tenant_id, insumo_id)       WHERE insumo_id IS NOT NULL;
CREATE INDEX idx_tareas_elaboracion  ON tareas(tenant_id, elaboracion_id)  WHERE elaboracion_id IS NOT NULL;
CREATE INDEX idx_tareas_orden_compra ON tareas(tenant_id, orden_compra_id) WHERE orden_compra_id IS NOT NULL;
CREATE INDEX idx_tareas_venta        ON tareas(tenant_id, venta_id)        WHERE venta_id IS NOT NULL;
