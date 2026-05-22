-- Vincula facturas históricas a la entidad Proveedor por coincidencia de nombre.
-- Solo afecta filas donde proveedor_id IS NULL y el nombre del campo texto
-- coincide exactamente (case-insensitive) con un proveedor activo.
-- El campo proveedor (texto) se preserva intacto para compatibilidad.
UPDATE facturas_proveedor fp
SET proveedor_id = p.id
FROM proveedores p
WHERE fp.proveedor_id IS NULL
  AND LOWER(TRIM(fp.proveedor)) = LOWER(TRIM(p.nombre))
  AND p.activo = true;