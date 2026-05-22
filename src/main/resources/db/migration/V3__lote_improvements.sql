-- ============================================================
-- Alera V3: Notas de cata + vinculación lote-receta
-- ============================================================

ALTER TABLE lotes_cerveza
    ADD COLUMN IF NOT EXISTS notas_cata TEXT,
    ADD COLUMN IF NOT EXISTS receta_id  BIGINT REFERENCES recetas(id) ON DELETE SET NULL;
