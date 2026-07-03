-- V66: Renombrar columnas de multi-elaboración (antes llamadas "coccion")
ALTER TABLE lotes_cerveza RENAME COLUMN numero_cocciones              TO numero_elaboraciones;
ALTER TABLE lotes_cerveza RENAME COLUMN fecha_segunda_coccion         TO fecha_segunda_elaboracion;
ALTER TABLE lotes_cerveza RENAME COLUMN agua_segunda_coccion          TO agua_segunda_elaboracion;
ALTER TABLE lotes_cerveza RENAME COLUMN og_segunda_coccion            TO og_segunda_elaboracion;
ALTER TABLE lotes_cerveza RENAME COLUMN og_brix_segunda_coccion       TO og_brix_segunda_elaboracion;
ALTER TABLE lotes_cerveza RENAME COLUMN fecha_tercera_coccion         TO fecha_tercera_elaboracion;
ALTER TABLE lotes_cerveza RENAME COLUMN agua_tercera_coccion          TO agua_tercera_elaboracion;
ALTER TABLE lotes_cerveza RENAME COLUMN og_tercera_coccion            TO og_tercera_elaboracion;
ALTER TABLE lotes_cerveza RENAME COLUMN og_brix_tercera_coccion       TO og_brix_tercera_elaboracion;
ALTER TABLE lotes_cerveza RENAME COLUMN og_primera_coccion            TO og_primera_elaboracion;
ALTER TABLE lotes_cerveza RENAME COLUMN volumen_final_primera_coccion TO volumen_final_primera_elaboracion;
ALTER TABLE lotes_cerveza RENAME COLUMN volumen_final_segunda_coccion TO volumen_final_segunda_elaboracion;
ALTER TABLE lotes_cerveza RENAME COLUMN volumen_final_tercera_coccion TO volumen_final_tercera_elaboracion;
