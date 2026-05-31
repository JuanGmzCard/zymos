-- Soft delete para ventas
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_ventas_deleted ON ventas (deleted_at) WHERE deleted_at IS NULL;
