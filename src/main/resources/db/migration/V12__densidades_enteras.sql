-- Convierte densidades de DECIMAL(8,3) a INTEGER (formato XXXX, ej: 1.056 → 1056)
ALTER TABLE lotes_cerveza
    ALTER COLUMN densidad_inicial TYPE INTEGER
        USING CASE WHEN densidad_inicial IS NULL THEN NULL
                   ELSE ROUND(densidad_inicial * 1000)::INTEGER END,
    ALTER COLUMN densidad_final TYPE INTEGER
        USING CASE WHEN densidad_final IS NULL THEN NULL
                   ELSE ROUND(densidad_final * 1000)::INTEGER END;

-- Convierte OG/FG objetivo de recetas (DECIMAL → INTEGER)
ALTER TABLE recetas
    ALTER COLUMN og_objetivo TYPE INTEGER
        USING CASE WHEN og_objetivo IS NULL THEN NULL
                   ELSE ROUND(og_objetivo * 1000)::INTEGER END,
    ALTER COLUMN fg_objetivo TYPE INTEGER
        USING CASE WHEN fg_objetivo IS NULL THEN NULL
                   ELSE ROUND(fg_objetivo * 1000)::INTEGER END;