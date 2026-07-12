-- V77: Referencias de tareas a entidades comerciales (prioridad media)
ALTER TABLE tareas
    ADD COLUMN cliente_id   BIGINT REFERENCES clientes(id)            ON DELETE SET NULL,
    ADD COLUMN factura_id   BIGINT REFERENCES facturas_proveedor(id)  ON DELETE SET NULL,
    ADD COLUMN proveedor_id BIGINT REFERENCES proveedores(id)          ON DELETE SET NULL;

CREATE INDEX idx_tareas_cliente   ON tareas(tenant_id, cliente_id)   WHERE cliente_id   IS NOT NULL;
CREATE INDEX idx_tareas_factura   ON tareas(tenant_id, factura_id)   WHERE factura_id   IS NOT NULL;
CREATE INDEX idx_tareas_proveedor ON tareas(tenant_id, proveedor_id) WHERE proveedor_id IS NOT NULL;
