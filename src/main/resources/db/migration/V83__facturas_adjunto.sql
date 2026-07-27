-- V83: adjunto de archivo en facturas de proveedor
ALTER TABLE facturas_proveedor
    ADD COLUMN archivo_adjunto VARCHAR(500),
    ADD COLUMN archivo_nombre  VARCHAR(255);
