-- Elimina el CHECK constraint de V36 que solo permitía nombres de enum (MALTA, LUPULO...).
-- Tras V45, insumos_inventario.tipo es texto libre con nombres display (Malta, Lúpulo...).
-- El constraint bloqueaba inserts del servicio de migración y de nuevos registros en general.
-- También repite los UPDATEs de V45 como idempotente por si V45 no pudo ejecutarlos.

ALTER TABLE insumos_inventario
    DROP CONSTRAINT IF EXISTS insumos_inventario_tipo_check;

UPDATE insumos_inventario SET tipo = 'Malta'                   WHERE tipo = 'MALTA';
UPDATE insumos_inventario SET tipo = 'Lúpulo'                  WHERE tipo = 'LUPULO';
UPDATE insumos_inventario SET tipo = 'Levadura'                WHERE tipo = 'LEVADURA';
UPDATE insumos_inventario SET tipo = 'Clarificante'            WHERE tipo = 'CLARIFICANTE';
UPDATE insumos_inventario SET tipo = 'Agente de Carbonatación' WHERE tipo = 'AGENTE_CARBONATACION';
UPDATE insumos_inventario SET tipo = 'Agua'                    WHERE tipo = 'AGUA';
UPDATE insumos_inventario SET tipo = 'Químico'                 WHERE tipo = 'QUIMICO';
UPDATE insumos_inventario SET tipo = 'Envase'                  WHERE tipo = 'ENVASE';
UPDATE insumos_inventario SET tipo = 'Otro'                    WHERE tipo = 'OTRO';
