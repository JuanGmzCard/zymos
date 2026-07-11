-- V74: Columnas para la cuarta sesión de elaboración
ALTER TABLE lotes_cerveza
    ADD COLUMN IF NOT EXISTS fecha_cuarta_elaboracion          DATE,
    ADD COLUMN IF NOT EXISTS agua_cuarta_elaboracion           NUMERIC(10,3),
    ADD COLUMN IF NOT EXISTS og_cuarta_elaboracion             INTEGER,
    ADD COLUMN IF NOT EXISTS og_brix_cuarta_elaboracion        NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS volumen_final_cuarta_elaboracion  NUMERIC(10,3),
    ADD COLUMN IF NOT EXISTS hora_inicio_cuarta_elaboracion    TIME,
    ADD COLUMN IF NOT EXISTS hora_fin_cuarta_elaboracion       TIME,
    ADD COLUMN IF NOT EXISTS receta4_id                        BIGINT REFERENCES recetas(id);
