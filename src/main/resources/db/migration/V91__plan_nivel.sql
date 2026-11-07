-- Nivel de plan comercial por tenant (MACERADO / COCCION / FERMENTACION)
-- Valor null = acceso completo (tenants pre-V91 no pierden acceso)
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS plan_nivel VARCHAR(20);
