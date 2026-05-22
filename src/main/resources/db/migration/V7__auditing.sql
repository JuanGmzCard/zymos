-- ============================================================
-- Alera V7: Campos de auditoría JPA en lotes_cerveza
-- ============================================================

ALTER TABLE lotes_cerveza
    ADD COLUMN IF NOT EXISTS last_modified_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_modified_by  VARCHAR(100);
