-- Ampliar columnas firma existentes (VARCHAR→TEXT para almacenar base64 PNG)
ALTER TABLE bpm_registros_sintomas
    ALTER COLUMN firma_manipulador TYPE TEXT,
    ALTER COLUMN firma_responsable TYPE TEXT;

ALTER TABLE bpm_avistamiento_plagas
    ALTER COLUMN firma TYPE TEXT;

-- Agregar columna firma a tablas que no la tienen
ALTER TABLE bpm_soluciones_desinfectantes ADD COLUMN IF NOT EXISTS firma TEXT;
ALTER TABLE bpm_evacuacion_residuos      ADD COLUMN IF NOT EXISTS firma TEXT;
ALTER TABLE bpm_limpieza_desinfeccion    ADD COLUMN IF NOT EXISTS firma TEXT;
