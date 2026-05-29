-- Agrega AGENTE_CARBONATACION a los CHECK constraints de tipo en insumos e ítems de factura
-- IF EXISTS porque los constraints fueron creados por Hibernate en BD existentes (no por migración previa)

ALTER TABLE insumos_inventario
    DROP CONSTRAINT IF EXISTS insumos_inventario_tipo_check;
ALTER TABLE insumos_inventario
    ADD CONSTRAINT insumos_inventario_tipo_check
        CHECK (tipo IN ('MALTA','LUPULO','LEVADURA','CLARIFICANTE','AGENTE_CARBONATACION','AGUA','QUIMICO','ENVASE','OTRO'));

ALTER TABLE factura_items
    DROP CONSTRAINT IF EXISTS factura_items_tipo_insumo_check;
ALTER TABLE factura_items
    ADD CONSTRAINT factura_items_tipo_insumo_check
        CHECK (tipo_insumo IN ('MALTA','LUPULO','LEVADURA','CLARIFICANTE','AGENTE_CARBONATACION','AGUA','QUIMICO','ENVASE','OTRO'));
