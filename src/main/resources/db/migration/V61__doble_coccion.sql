-- V60: Soporte para lotes elaborados en dos sesiones de cocción (doble batch)
ALTER TABLE lotes_cerveza
    ADD COLUMN IF NOT EXISTS numero_cocciones      INTEGER       NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS fecha_segunda_coccion DATE,
    ADD COLUMN IF NOT EXISTS agua_segunda_coccion  NUMERIC(10,3),
    ADD COLUMN IF NOT EXISTS og_segunda_coccion    INTEGER;
