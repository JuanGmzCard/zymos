-- V52: Agrega período de vigencia al plan del tenant
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS plan_tipo  VARCHAR(20);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS plan_inicio DATE;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS plan_fin    DATE;
