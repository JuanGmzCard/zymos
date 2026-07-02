-- V64: Ampliar precisión de campos numéricos que pueden superar 9.999.999 al acumular cocciones múltiples

-- lote_items_factura: cantidad asignada puede duplicarse/triplicarse con múltiples cocciones
ALTER TABLE lote_items_factura
    ALTER COLUMN cantidad_asignada TYPE NUMERIC(14, 3);

-- insumos_inventario: el stock puede ser muy alto en cervecerías grandes (valor en unidad base: gr o mL)
ALTER TABLE insumos_inventario
    ALTER COLUMN cantidad     TYPE NUMERIC(14, 3),
    ALTER COLUMN stock_minimo TYPE NUMERIC(14, 3);

-- movimientos_inventario: refleja los mismos valores que insumos
ALTER TABLE movimientos_inventario
    ALTER COLUMN cantidad          TYPE NUMERIC(14, 3),
    ALTER COLUMN cantidad_anterior TYPE NUMERIC(14, 3),
    ALTER COLUMN cantidad_posterior TYPE NUMERIC(14, 3);

-- lotes_cerveza: agua y litros por seguridad
ALTER TABLE lotes_cerveza
    ALTER COLUMN agua_utilizada        TYPE NUMERIC(14, 3),
    ALTER COLUMN litros_finales        TYPE NUMERIC(14, 3),
    ALTER COLUMN agua_segunda_coccion  TYPE NUMERIC(14, 3),
    ALTER COLUMN agua_tercera_coccion  TYPE NUMERIC(14, 3);
