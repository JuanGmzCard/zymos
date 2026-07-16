-- V82: símbolo de moneda por tenant (Fase 1 multi-currency)
-- Cada tenant puede configurar su propio símbolo ($, €, £, COP $, etc.)
ALTER TABLE tenants
    ADD COLUMN simbolo_moneda VARCHAR(10) NOT NULL DEFAULT '$';
