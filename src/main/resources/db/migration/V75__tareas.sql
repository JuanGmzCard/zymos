-- Módulo de Tareas: tabla principal + ítems por tarea
CREATE TABLE tareas (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    titulo          VARCHAR(200) NOT NULL,
    descripcion     TEXT,
    fecha_vencimiento DATE,
    prioridad       VARCHAR(20)  NOT NULL DEFAULT 'MEDIA',
    estado          VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE',
    asignado_a      VARCHAR(100),
    creado_por      VARCHAR(100),
    lote_id         BIGINT REFERENCES lotes_cerveza(id) ON DELETE SET NULL,
    equipo_id       BIGINT REFERENCES equipos(id)       ON DELETE SET NULL,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_tareas_tenant       ON tareas(tenant_id);
CREATE INDEX idx_tareas_tenant_estado ON tareas(tenant_id, estado);
CREATE INDEX idx_tareas_tenant_asig   ON tareas(tenant_id, asignado_a);

CREATE TABLE tarea_items (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    tarea_id    BIGINT       NOT NULL REFERENCES tareas(id) ON DELETE CASCADE,
    descripcion VARCHAR(500) NOT NULL,
    completado  BOOLEAN      NOT NULL DEFAULT FALSE,
    orden_item  INTEGER      NOT NULL DEFAULT 0,
    lote_id     BIGINT REFERENCES lotes_cerveza(id) ON DELETE SET NULL,
    equipo_id   BIGINT REFERENCES equipos(id)       ON DELETE SET NULL
);

CREATE INDEX idx_tarea_items_tenant ON tarea_items(tenant_id);
CREATE INDEX idx_tarea_items_tarea  ON tarea_items(tarea_id);
