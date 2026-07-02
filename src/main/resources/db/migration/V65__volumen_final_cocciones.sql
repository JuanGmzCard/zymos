ALTER TABLE lotes_cerveza
    ADD COLUMN IF NOT EXISTS og_primera_coccion            INTEGER,
    ADD COLUMN IF NOT EXISTS volumen_final_primera_coccion NUMERIC(14,3),
    ADD COLUMN IF NOT EXISTS volumen_final_segunda_coccion NUMERIC(14,3),
    ADD COLUMN IF NOT EXISTS volumen_final_tercera_coccion NUMERIC(14,3),
    ADD COLUMN IF NOT EXISTS og_brix_segunda_coccion       NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS og_brix_tercera_coccion       NUMERIC(5,2);
