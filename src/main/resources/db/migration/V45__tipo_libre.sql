-- Convierte valores de enum a nombres de display en insumos_inventario y equipos
-- El schema no cambia (ya es VARCHAR por @Enumerated(EnumType.STRING))

UPDATE insumos_inventario SET tipo = 'Malta'                  WHERE tipo = 'MALTA';
UPDATE insumos_inventario SET tipo = 'Lúpulo'                 WHERE tipo = 'LUPULO';
UPDATE insumos_inventario SET tipo = 'Levadura'               WHERE tipo = 'LEVADURA';
UPDATE insumos_inventario SET tipo = 'Clarificante'           WHERE tipo = 'CLARIFICANTE';
UPDATE insumos_inventario SET tipo = 'Agente de Carbonatación' WHERE tipo = 'AGENTE_CARBONATACION';
UPDATE insumos_inventario SET tipo = 'Agua'                   WHERE tipo = 'AGUA';
UPDATE insumos_inventario SET tipo = 'Químico'                WHERE tipo = 'QUIMICO';
UPDATE insumos_inventario SET tipo = 'Envase'                 WHERE tipo = 'ENVASE';
UPDATE insumos_inventario SET tipo = 'Otro'                   WHERE tipo = 'OTRO';

UPDATE equipos SET tipo = 'Fermentador'      WHERE tipo = 'FERMENTADOR';
UPDATE equipos SET tipo = 'Olla de Macerado' WHERE tipo = 'OLLA_MACERADO';
UPDATE equipos SET tipo = 'Olla de Hervor'   WHERE tipo = 'OLLA_HERVOR';
UPDATE equipos SET tipo = 'Enfriador'        WHERE tipo = 'ENFRIADOR';
UPDATE equipos SET tipo = 'Bomba'            WHERE tipo = 'BOMBA';
UPDATE equipos SET tipo = 'Filtro'           WHERE tipo = 'FILTRO';
UPDATE equipos SET tipo = 'Medidor de pH'    WHERE tipo = 'MEDIDOR_PH';
UPDATE equipos SET tipo = 'Densímetro'       WHERE tipo = 'DENSIMETRO';
UPDATE equipos SET tipo = 'Báscula'          WHERE tipo = 'BASCULA';
UPDATE equipos SET tipo = 'Compresor'        WHERE tipo = 'COMPRESOR';
UPDATE equipos SET tipo = 'Otro'             WHERE tipo = 'OTRO';
