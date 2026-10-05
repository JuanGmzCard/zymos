-- V89: Tabla de comentarios por tarea
CREATE TABLE tarea_comentarios (
    id          BIGSERIAL     PRIMARY KEY,
    tarea_id    BIGINT        NOT NULL,
    texto       TEXT          NOT NULL,
    autor       VARCHAR(100)  NOT NULL,
    fecha_hora  TIMESTAMP     NOT NULL DEFAULT NOW(),
    tenant_id   VARCHAR(100)  NOT NULL,
    CONSTRAINT fk_tcom_tarea FOREIGN KEY (tarea_id) REFERENCES tareas(id) ON DELETE CASCADE
);

CREATE INDEX idx_tcom_tarea_id ON tarea_comentarios(tarea_id);