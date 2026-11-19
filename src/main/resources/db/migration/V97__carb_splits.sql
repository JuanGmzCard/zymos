-- V97: tabla de divisiones de carbonatación (CarbSplit)
-- Permite dividir un lote en múltiples métodos/secciones de carbonatación

CREATE TABLE IF NOT EXISTS carb_splits (
    id         BIGSERIAL PRIMARY KEY,
    lote_id    BIGINT NOT NULL REFERENCES lotes_cerveza(id) ON DELETE CASCADE,
    tenant_id  VARCHAR(100),
    orden      INTEGER NOT NULL DEFAULT 0,
    volumen    NUMERIC(10,3),
    metodo     VARCHAR(20),
    azucar_tipo   VARCHAR(200),
    azucar_gramos NUMERIC(10,2),
    presion_psi   NUMERIC(6,2),
    tiempo_horas  INTEGER,
    tecnica       VARCHAR(50),
    co2_objetivo  NUMERIC(4,2),
    co2_real      NUMERIC(4,2),
    notas         TEXT
);

CREATE INDEX IF NOT EXISTS idx_carb_splits_lote   ON carb_splits(lote_id);
CREATE INDEX IF NOT EXISTS idx_carb_splits_tenant ON carb_splits(tenant_id);
