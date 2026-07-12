-- V79: Tabla tarea_referencias para soporte de múltiples referencias por tarea
-- Reemplaza las 11 columnas FK individuales con una colección arbitraria

CREATE TABLE tarea_referencias (
    id          BIGSERIAL    PRIMARY KEY,
    tarea_id    BIGINT       NOT NULL,
    tipo        VARCHAR(30)  NOT NULL,
    entidad_id  BIGINT       NOT NULL,
    label       TEXT         NOT NULL DEFAULT '',
    url         VARCHAR(500) NOT NULL DEFAULT '',
    orden       INTEGER      NOT NULL DEFAULT 0,
    tenant_id   VARCHAR(100) NOT NULL,
    CONSTRAINT fk_tref_tarea FOREIGN KEY (tarea_id) REFERENCES tareas(id) ON DELETE CASCADE
);

CREATE INDEX idx_tref_tarea_id ON tarea_referencias(tarea_id);

-- Migrar datos existentes desde columnas FK individuales

INSERT INTO tarea_referencias (tarea_id, tipo, entidad_id, label, url, tenant_id)
SELECT t.id, 'LOTE', t.lote_id,
    l.codigo_lote || CASE WHEN COALESCE(l.estilo,'') <> '' THEN ' — ' || l.estilo ELSE '' END,
    '/ver/' || t.lote_id, t.tenant_id
FROM tareas t JOIN lotes_cerveza l ON l.id = t.lote_id AND l.tenant_id = t.tenant_id
WHERE t.lote_id IS NOT NULL;

INSERT INTO tarea_referencias (tarea_id, tipo, entidad_id, label, url, tenant_id)
SELECT t.id, 'EQUIPO', t.equipo_id, e.nombre, '/equipos/ver/' || t.equipo_id, t.tenant_id
FROM tareas t JOIN equipos e ON e.id = t.equipo_id AND e.tenant_id = t.tenant_id
WHERE t.equipo_id IS NOT NULL;

INSERT INTO tarea_referencias (tarea_id, tipo, entidad_id, label, url, tenant_id)
SELECT t.id, 'INSUMO', t.insumo_id,
    i.nombre || CASE WHEN COALESCE(i.tipo,'') <> '' THEN ' (' || i.tipo || ')' ELSE '' END,
    '/inventario/' || t.insumo_id || '/historial', t.tenant_id
FROM tareas t JOIN insumos_inventario i ON i.id = t.insumo_id AND i.tenant_id = t.tenant_id
WHERE t.insumo_id IS NOT NULL;

INSERT INTO tarea_referencias (tarea_id, tipo, entidad_id, label, url, tenant_id)
SELECT t.id, 'ELABORACION', t.elaboracion_id, e.nombre_elaboracion, '/planificacion', t.tenant_id
FROM tareas t JOIN elaboraciones_planificadas e ON e.id = t.elaboracion_id AND e.tenant_id = t.tenant_id
WHERE t.elaboracion_id IS NOT NULL;

INSERT INTO tarea_referencias (tarea_id, tipo, entidad_id, label, url, tenant_id)
SELECT t.id, 'ORDEN_COMPRA', t.orden_compra_id,
    COALESCE(o.numero_oc, 'OC sin número') || CASE WHEN COALESCE(o.proveedor,'') <> '' THEN ' — ' || o.proveedor ELSE '' END,
    '/ordenes-compra/ver/' || t.orden_compra_id, t.tenant_id
FROM tareas t JOIN ordenes_compra o ON o.id = t.orden_compra_id AND o.tenant_id = t.tenant_id
WHERE t.orden_compra_id IS NOT NULL;

INSERT INTO tarea_referencias (tarea_id, tipo, entidad_id, label, url, tenant_id)
SELECT t.id, 'VENTA', t.venta_id,
    COALESCE(v.cliente,'') || CASE WHEN COALESCE(v.remision_numero,'') <> '' THEN ' #' || v.remision_numero ELSE '' END,
    '/ventas/ver/' || t.venta_id, t.tenant_id
FROM tareas t JOIN ventas v ON v.id = t.venta_id AND v.tenant_id = t.tenant_id
WHERE t.venta_id IS NOT NULL;

INSERT INTO tarea_referencias (tarea_id, tipo, entidad_id, label, url, tenant_id)
SELECT t.id, 'CLIENTE', t.cliente_id,
    c.nombre || CASE WHEN COALESCE(c.nit,'') <> '' THEN ' — ' || c.nit ELSE '' END,
    '/clientes/ver/' || t.cliente_id, t.tenant_id
FROM tareas t JOIN clientes c ON c.id = t.cliente_id AND c.tenant_id = t.tenant_id
WHERE t.cliente_id IS NOT NULL;

INSERT INTO tarea_referencias (tarea_id, tipo, entidad_id, label, url, tenant_id)
SELECT t.id, 'FACTURA', t.factura_id,
    CASE WHEN COALESCE(f.numero_factura,'') <> '' THEN f.numero_factura ELSE '#' || f.id END
    || CASE WHEN COALESCE(f.proveedor,'') <> '' THEN ' — ' || f.proveedor ELSE '' END,
    '/facturas/ver/' || t.factura_id, t.tenant_id
FROM tareas t JOIN facturas_proveedor f ON f.id = t.factura_id AND f.tenant_id = t.tenant_id
WHERE t.factura_id IS NOT NULL;

INSERT INTO tarea_referencias (tarea_id, tipo, entidad_id, label, url, tenant_id)
SELECT t.id, 'PROVEEDOR', t.proveedor_id,
    p.nombre || CASE WHEN COALESCE(p.nit,'') <> '' THEN ' — ' || p.nit ELSE '' END,
    '/proveedores/editar/' || t.proveedor_id, t.tenant_id
FROM tareas t JOIN proveedores p ON p.id = t.proveedor_id AND p.tenant_id = t.tenant_id
WHERE t.proveedor_id IS NOT NULL;

INSERT INTO tarea_referencias (tarea_id, tipo, entidad_id, label, url, tenant_id)
SELECT t.id, 'RECETA', t.receta_id,
    r.nombre || CASE WHEN COALESCE(r.estilo,'') <> '' THEN ' — ' || r.estilo ELSE '' END,
    '/recetas/ver/' || t.receta_id, t.tenant_id
FROM tareas t JOIN recetas r ON r.id = t.receta_id AND r.tenant_id = t.tenant_id
WHERE t.receta_id IS NOT NULL;

INSERT INTO tarea_referencias (tarea_id, tipo, entidad_id, label, url, tenant_id)
SELECT t.id, 'BARRIL', t.barril_id,
    b.codigo || CASE WHEN COALESCE(b.tipo,'') <> '' THEN ' — ' || b.tipo ELSE '' END,
    '/barriles/ver/' || t.barril_id, t.tenant_id
FROM tareas t JOIN barriles b ON b.id = t.barril_id AND b.tenant_id = t.tenant_id
WHERE t.barril_id IS NOT NULL;
