ALTER TABLE bpm_registros_sintomas
    ADD COLUMN IF NOT EXISTS autorizado_por_admin BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS autorizado_por        VARCHAR(100);
