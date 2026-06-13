-- Soporte para mediciones con refractómetro (°Brix) y corrección de Sean Terrill
-- og_brix: lectura en °Brix del mosto sin fermentar
-- fg_brix: lectura cruda del refractómetro del mosto fermentado (sin corregir — Terrill la corrige en código)
ALTER TABLE lotes_cerveza
    ADD COLUMN IF NOT EXISTS og_brix DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS fg_brix DECIMAL(5,2);
