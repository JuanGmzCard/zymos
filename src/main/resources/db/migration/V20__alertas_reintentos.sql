-- Tracking de fallos consecutivos en el envío de alertas email por tenant.
-- Permite al admin visualizar tenants con problemas de SMTP y al scheduler
-- emitir warnings escalados tras MAX_REINTENTOS_WARN intentos fallidos.
ALTER TABLE tenants ADD COLUMN alertas_intentos_fallidos INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tenants ADD COLUMN alertas_ultimo_intento    TIMESTAMP;
ALTER TABLE tenants ADD COLUMN alertas_ultimo_exito      TIMESTAMP;
