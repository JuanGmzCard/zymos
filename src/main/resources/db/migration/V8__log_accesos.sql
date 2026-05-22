-- ============================================================
-- Alera V8: Log de accesos y eventos de seguridad
-- ============================================================

CREATE TABLE IF NOT EXISTS log_accesos (
    id        BIGSERIAL    PRIMARY KEY,
    usuario   VARCHAR(100),
    tipo      VARCHAR(30)  NOT NULL,   -- LOGIN_OK, LOGIN_FALLIDO, ACCESO_DENEGADO
    ip        VARCHAR(80),
    url       VARCHAR(500),
    user_agent VARCHAR(300),
    fecha     TIMESTAMP    NOT NULL,
    detalles  TEXT
);

CREATE INDEX IF NOT EXISTS idx_log_accesos_fecha   ON log_accesos(fecha DESC);
CREATE INDEX IF NOT EXISTS idx_log_accesos_usuario ON log_accesos(usuario);
CREATE INDEX IF NOT EXISTS idx_log_accesos_tipo    ON log_accesos(tipo);
