-- Soft delete: marcar como eliminado en lugar de borrar físicamente.
-- deleted_at NULL = activo, NOT NULL = archivado (fecha de archivado)
ALTER TABLE lotes_cerveza ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE recetas        ADD COLUMN deleted_at TIMESTAMP;
