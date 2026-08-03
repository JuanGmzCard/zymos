-- V84: unidad de temperatura por tenant (C/F) + campos OG/FG en lotes
ALTER TABLE tenants
    ADD COLUMN unidad_temperatura VARCHAR(1) NOT NULL DEFAULT 'C';

ALTER TABLE lotes_cerveza
    ADD COLUMN og_temperatura NUMERIC(5,2),
    ADD COLUMN fg_temperatura NUMERIC(5,2);