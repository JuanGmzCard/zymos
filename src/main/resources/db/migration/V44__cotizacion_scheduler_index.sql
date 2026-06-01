-- Índice de performance para el scheduler que expira cotizaciones vencidas
CREATE INDEX IF NOT EXISTS idx_ventas_expirar_scheduler
    ON ventas (cotizacion_expira_en)
    WHERE estado = 'COTIZACION' AND deleted_at IS NULL;
