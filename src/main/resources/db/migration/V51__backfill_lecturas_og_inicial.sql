-- Backfill: primer registro de curva de fermentación para lotes existentes sin lecturas.
-- Inserta OG (densidad_inicial) + temperatura de fermentación en lecturas_fermentacion
-- para cada lote activo que tenga densidad_inicial y aún no tenga ninguna lectura registrada.

INSERT INTO lecturas_fermentacion (lote_id, fecha, densidad, temperatura, tenant_id)
SELECT
    l.id,
    COALESCE(l.ferm_fecha_inicial, l.fecha_elaboracion) AS fecha,
    l.densidad_inicial                                   AS densidad,
    l.ferm_temperatura                                   AS temperatura,
    l.tenant_id
FROM lotes_cerveza l
WHERE l.densidad_inicial IS NOT NULL
  AND l.deleted_at IS NULL
  AND COALESCE(l.ferm_fecha_inicial, l.fecha_elaboracion) IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM lecturas_fermentacion lf WHERE lf.lote_id = l.id
  );
