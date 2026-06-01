ALTER TABLE ventas
    ADD COLUMN IF NOT EXISTS cliente_id           BIGINT REFERENCES clientes(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS remision_numero      VARCHAR(20),
    ADD COLUMN IF NOT EXISTS cotizacion_expira_en DATE;

-- remisionNumero único por tenant (cuando está asignado)
CREATE UNIQUE INDEX idx_ventas_remision_tenant
    ON ventas (tenant_id, remision_numero)
    WHERE remision_numero IS NOT NULL;

CREATE INDEX idx_ventas_cliente_ref
    ON ventas (cliente_id)
    WHERE cliente_id IS NOT NULL;

CREATE INDEX idx_ventas_cotizacion
    ON ventas (tenant_id, cotizacion_expira_en, estado)
    WHERE estado = 'COTIZACION';
