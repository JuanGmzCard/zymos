-- V63: Receta por sesión de cocción adicional
ALTER TABLE lotes_cerveza
    ADD COLUMN IF NOT EXISTS receta2_id BIGINT REFERENCES recetas(id),
    ADD COLUMN IF NOT EXISTS receta3_id BIGINT REFERENCES recetas(id);
