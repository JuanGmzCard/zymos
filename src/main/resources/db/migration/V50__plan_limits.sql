ALTER TABLE tenants ADD COLUMN IF NOT EXISTS max_lotes   INTEGER;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS max_usuarios INTEGER;
-- NULL = sin límite (ilimitado)
