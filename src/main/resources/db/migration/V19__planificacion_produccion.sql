-- V19: Planificación de producción
-- Elaboraciones planificadas con receta opcional, volumen estimado y estado de seguimiento.

CREATE TABLE elaboraciones_planificadas (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           VARCHAR(100) NOT NULL DEFAULT 'default',
    fecha_planeada      DATE         NOT NULL,
    receta_id           BIGINT       REFERENCES recetas(id) ON DELETE SET NULL,
    nombre_elaboracion  VARCHAR(150) NOT NULL,
    volumen_estimado    DECIMAL(10,2),
    estado              VARCHAR(20)  NOT NULL DEFAULT 'PLANIFICADA',
    notas               VARCHAR(500),
    creado_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_elab_plan_tenant ON elaboraciones_planificadas(tenant_id);
CREATE INDEX idx_elab_plan_fecha  ON elaboraciones_planificadas(fecha_planeada, tenant_id);
