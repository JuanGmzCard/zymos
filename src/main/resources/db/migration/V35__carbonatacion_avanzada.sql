-- Campos avanzados de carbonatación según guía de trazabilidad
-- Capa 1: Variables iniciales
-- Capa 2: Método condicional (Natural / Forzada)
-- Capa 3: Control de calidad (CO₂ real, validación organoléptica, destino)
ALTER TABLE lotes_cerveza
    ADD COLUMN IF NOT EXISTS carb_metodo        VARCHAR(20),
    ADD COLUMN IF NOT EXISTS carb_co2_objetivo  DECIMAL(4,2),
    ADD COLUMN IF NOT EXISTS carb_co2_real      DECIMAL(4,2),
    ADD COLUMN IF NOT EXISTS carb_azucar_tipo   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS carb_azucar_gramos DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS carb_presion_psi   DECIMAL(6,2),
    ADD COLUMN IF NOT EXISTS carb_tiempo_horas  INTEGER,
    ADD COLUMN IF NOT EXISTS carb_tecnica       VARCHAR(50),
    ADD COLUMN IF NOT EXISTS carb_validacion    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS carb_destino       VARCHAR(300);