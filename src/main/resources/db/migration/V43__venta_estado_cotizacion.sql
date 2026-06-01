-- Ampliar CHECK constraint de estado en ventas para incluir COTIZACION y EXPIRADO
ALTER TABLE ventas DROP CONSTRAINT IF EXISTS chk_ventas_estado;
ALTER TABLE ventas ADD CONSTRAINT chk_ventas_estado
    CHECK (estado IN ('COTIZACION', 'PENDIENTE', 'DESPACHADO', 'CANCELADO', 'EXPIRADO'));
