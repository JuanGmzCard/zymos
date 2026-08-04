-- V85: Renombrar lecturas OG huérfanas a 'OG inicial'
-- El controller anterior creaba una lectura con notas=NULL al crear un lote.
-- El nuevo flujo usa upsert buscando notas='OG inicial'. Sin esta migración,
-- editar un lote existente que cambie el OG generaría una lectura duplicada.
-- Se elige la primera lectura (menor id) por lote cuya densidad coincida con
-- densidad_inicial del lote y tenga notas IS NULL.

UPDATE lecturas_fermentacion lf
SET notas = 'OG inicial'
WHERE lf.notas IS NULL
  AND lf.id IN (
      SELECT DISTINCT ON (lf2.lote_id) lf2.id
      FROM lecturas_fermentacion lf2
      JOIN lotes_cerveza lc ON lf2.lote_id = lc.id
                            AND lf2.tenant_id = lc.tenant_id
      WHERE lf2.notas IS NULL
        AND lf2.densidad = lc.densidad_inicial
      ORDER BY lf2.lote_id, lf2.id ASC
  );
