-- V53: Tabla de evaluaciones sensoriales por lote (categorías BJCP — puntaje total /50)
CREATE TABLE evaluaciones_sensoriales (
    id                BIGSERIAL    PRIMARY KEY,
    tenant_id         VARCHAR(100) NOT NULL DEFAULT 'default',
    lote_id           BIGINT       NOT NULL REFERENCES lotes_cerveza(id) ON DELETE CASCADE,
    fecha             DATE         NOT NULL,
    catador           VARCHAR(100),
    aroma             INTEGER      CHECK (aroma BETWEEN 0 AND 12),
    apariencia        INTEGER      CHECK (apariencia BETWEEN 0 AND 3),
    sabor             INTEGER      CHECK (sabor BETWEEN 0 AND 20),
    sensacion_boca    INTEGER      CHECK (sensacion_boca BETWEEN 0 AND 5),
    impresion_general INTEGER      CHECK (impresion_general BETWEEN 0 AND 10),
    notas             VARCHAR(1000),
    creado_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_eval_sens_lote_id ON evaluaciones_sensoriales (lote_id);
CREATE INDEX idx_eval_sens_tenant  ON evaluaciones_sensoriales (tenant_id);
