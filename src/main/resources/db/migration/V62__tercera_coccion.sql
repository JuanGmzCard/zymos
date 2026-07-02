-- V62: Columnas para la tercera sesión de cocción
ALTER TABLE lotes_cerveza
    ADD COLUMN IF NOT EXISTS fecha_tercera_coccion DATE,
    ADD COLUMN IF NOT EXISTS agua_tercera_coccion  NUMERIC(10,3),
    ADD COLUMN IF NOT EXISTS og_tercera_coccion    INTEGER;
