-- V93: Perfil de agua de proceso en recetas (iones en ppm)
ALTER TABLE recetas
    ADD COLUMN IF NOT EXISTS agua_calcio        INTEGER,
    ADD COLUMN IF NOT EXISTS agua_magnesio      INTEGER,
    ADD COLUMN IF NOT EXISTS agua_sodio         INTEGER,
    ADD COLUMN IF NOT EXISTS agua_sulfatos      INTEGER,
    ADD COLUMN IF NOT EXISTS agua_cloruros      INTEGER,
    ADD COLUMN IF NOT EXISTS agua_bicarbonatos  INTEGER,
    ADD COLUMN IF NOT EXISTS agua_notas         TEXT;

COMMENT ON COLUMN recetas.agua_calcio       IS 'Calcio (Ca²⁺) en ppm';
COMMENT ON COLUMN recetas.agua_magnesio     IS 'Magnesio (Mg²⁺) en ppm';
COMMENT ON COLUMN recetas.agua_sodio        IS 'Sodio (Na⁺) en ppm';
COMMENT ON COLUMN recetas.agua_sulfatos     IS 'Sulfatos (SO₄²⁻) en ppm';
COMMENT ON COLUMN recetas.agua_cloruros     IS 'Cloruros (Cl⁻) en ppm';
COMMENT ON COLUMN recetas.agua_bicarbonatos IS 'Bicarbonatos (HCO₃⁻) en ppm';
