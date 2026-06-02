-- Tablas de categorías de insumo y equipo gestionables desde la aplicación.
-- Reemplazan los enums TipoInsumo y TipoEquipo hardcodeados en el código.

CREATE TABLE tipos_insumo (
    id        BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    nombre    VARCHAR(100) NOT NULL,
    activo    BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (tenant_id, nombre)
);
CREATE INDEX idx_tipos_insumo_tenant ON tipos_insumo (tenant_id);

CREATE TABLE tipos_equipo (
    id        BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    nombre    VARCHAR(100) NOT NULL,
    activo    BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (tenant_id, nombre)
);
CREATE INDEX idx_tipos_equipo_tenant ON tipos_equipo (tenant_id);

-- Poblar con los valores actuales para cada tenant existente
INSERT INTO tipos_insumo (tenant_id, nombre)
SELECT t.subdomain, v.nombre
FROM tenants t
CROSS JOIN (VALUES
    ('Malta'), ('Lúpulo'), ('Levadura'), ('Clarificante'),
    ('Agente de Carbonatación'), ('Agua'), ('Químico'), ('Envase'), ('Otro')
) AS v(nombre)
ON CONFLICT DO NOTHING;

INSERT INTO tipos_equipo (tenant_id, nombre)
SELECT t.subdomain, v.nombre
FROM tenants t
CROSS JOIN (VALUES
    ('Fermentador'), ('Olla de Macerado'), ('Olla de Hervor'), ('Enfriador'),
    ('Bomba'), ('Filtro'), ('Medidor de pH'), ('Densímetro'), ('Báscula'), ('Compresor'), ('Otro')
) AS v(nombre)
ON CONFLICT DO NOTHING;

-- Eliminar CHECK constraints de factura_items que sólo permitían valores de enum
ALTER TABLE factura_items DROP CONSTRAINT IF EXISTS factura_items_tipo_insumo_check;
ALTER TABLE factura_items DROP CONSTRAINT IF EXISTS factura_items_tipo_equipo_check;

-- Convertir columnas de enum-name a String libre (valor ya es VARCHAR, sólo quitar constraint)
-- tipoInsumo en factura_items pasa de "MALTA" a "Malta" para las filas existentes
UPDATE factura_items SET tipo_insumo = 'Malta'                   WHERE tipo_insumo = 'MALTA';
UPDATE factura_items SET tipo_insumo = 'Lúpulo'                  WHERE tipo_insumo = 'LUPULO';
UPDATE factura_items SET tipo_insumo = 'Levadura'                WHERE tipo_insumo = 'LEVADURA';
UPDATE factura_items SET tipo_insumo = 'Clarificante'            WHERE tipo_insumo = 'CLARIFICANTE';
UPDATE factura_items SET tipo_insumo = 'Agente de Carbonatación' WHERE tipo_insumo = 'AGENTE_CARBONATACION';
UPDATE factura_items SET tipo_insumo = 'Agua'                    WHERE tipo_insumo = 'AGUA';
UPDATE factura_items SET tipo_insumo = 'Químico'                 WHERE tipo_insumo = 'QUIMICO';
UPDATE factura_items SET tipo_insumo = 'Envase'                  WHERE tipo_insumo = 'ENVASE';
UPDATE factura_items SET tipo_insumo = 'Otro'                    WHERE tipo_insumo = 'OTRO';

UPDATE factura_items SET tipo_equipo = 'Fermentador'      WHERE tipo_equipo = 'FERMENTADOR';
UPDATE factura_items SET tipo_equipo = 'Olla de Macerado' WHERE tipo_equipo = 'OLLA_MACERADO';
UPDATE factura_items SET tipo_equipo = 'Olla de Hervor'   WHERE tipo_equipo = 'OLLA_HERVOR';
UPDATE factura_items SET tipo_equipo = 'Enfriador'         WHERE tipo_equipo = 'ENFRIADOR';
UPDATE factura_items SET tipo_equipo = 'Bomba'             WHERE tipo_equipo = 'BOMBA';
UPDATE factura_items SET tipo_equipo = 'Filtro'            WHERE tipo_equipo = 'FILTRO';
UPDATE factura_items SET tipo_equipo = 'Medidor de pH'     WHERE tipo_equipo = 'MEDIDOR_PH';
UPDATE factura_items SET tipo_equipo = 'Densímetro'        WHERE tipo_equipo = 'DENSIMETRO';
UPDATE factura_items SET tipo_equipo = 'Báscula'           WHERE tipo_equipo = 'BASCULA';
UPDATE factura_items SET tipo_equipo = 'Compresor'         WHERE tipo_equipo = 'COMPRESOR';
UPDATE factura_items SET tipo_equipo = 'Otro'              WHERE tipo_equipo = 'OTRO';
