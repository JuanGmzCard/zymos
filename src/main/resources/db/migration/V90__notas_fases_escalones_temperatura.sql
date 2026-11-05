-- Notas libres por fase de proceso
ALTER TABLE lotes_cerveza
    ADD COLUMN notas_fermentacion    TEXT,
    ADD COLUMN notas_acondicionamiento TEXT,
    ADD COLUMN notas_maduracion      TEXT;

-- Escalones de temperatura para acondicionamiento y maduración
CREATE TABLE lote_escalones_temperatura (
    id                   BIGSERIAL    PRIMARY KEY,
    lote_id              BIGINT       NOT NULL REFERENCES lotes_cerveza(id) ON DELETE CASCADE,
    tenant_id            VARCHAR(100) NOT NULL,
    fase                 VARCHAR(20)  NOT NULL CHECK (fase IN ('ACONDICIONAMIENTO','MADURACION')),
    temperatura_objetivo NUMERIC(5,2) NOT NULL,
    fecha_objetivo       DATE,
    fecha_real           DATE,
    logrado              BOOLEAN      NOT NULL DEFAULT FALSE,
    notas                VARCHAR(500),
    orden                INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_lote_escalones_lote   ON lote_escalones_temperatura(lote_id);
CREATE INDEX idx_lote_escalones_tenant ON lote_escalones_temperatura(tenant_id);
