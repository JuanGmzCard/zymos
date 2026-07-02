-- V67: Hora de inicio y fin por sesión de elaboración
ALTER TABLE lotes_cerveza
    ADD COLUMN IF NOT EXISTS hora_inicio_primera_elaboracion TIME,
    ADD COLUMN IF NOT EXISTS hora_fin_primera_elaboracion    TIME,
    ADD COLUMN IF NOT EXISTS hora_inicio_segunda_elaboracion TIME,
    ADD COLUMN IF NOT EXISTS hora_fin_segunda_elaboracion    TIME,
    ADD COLUMN IF NOT EXISTS hora_inicio_tercera_elaboracion TIME,
    ADD COLUMN IF NOT EXISTS hora_fin_tercera_elaboracion    TIME;
