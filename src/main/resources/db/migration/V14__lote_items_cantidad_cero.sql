-- Allow 0 quantity: marks a cost-only item (e.g. packaging, overhead) associated to the batch
-- without contributing an ingredient quantity to the recipe.
-- 0 quantity => full item valorLinea is assigned to this batch.
ALTER TABLE lote_items_factura
    DROP CONSTRAINT IF EXISTS lote_items_factura_cantidad_asignada_check;

ALTER TABLE lote_items_factura
    ADD CONSTRAINT lote_items_factura_cantidad_asignada_check
    CHECK (cantidad_asignada >= 0);