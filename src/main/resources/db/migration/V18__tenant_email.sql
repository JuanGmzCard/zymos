-- V18: Campo de email de alertas por tenant
-- Si está configurado, el scheduler diario envía alertas de stock, vencimientos y mantenimiento.

ALTER TABLE tenants ADD COLUMN email_admin VARCHAR(200);