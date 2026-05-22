-- ============================================================
-- Alera V4: Historial de cambios por lote
-- Sin FK para preservar historia tras eliminación de lotes
-- ============================================================

CREATE TABLE historial_lotes (
    id          BIGSERIAL PRIMARY KEY,
    lote_id     BIGINT       NOT NULL,
    codigo_lote VARCHAR(50),
    accion      VARCHAR(20)  NOT NULL,
    usuario     VARCHAR(100),
    fecha       TIMESTAMP    NOT NULL,
    notas       TEXT
);

CREATE INDEX idx_historial_lote_id ON historial_lotes(lote_id);
