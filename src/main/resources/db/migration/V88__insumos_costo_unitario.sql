-- Agrega precio unitario a insumos de inventario para calcular
-- costo estimado de producción antes de asignar facturas reales.
ALTER TABLE insumos_inventario
    ADD COLUMN IF NOT EXISTS costo_unitario NUMERIC(14, 2);
