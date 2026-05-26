CREATE TABLE migracion_log (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    modulo      VARCHAR(50)  NOT NULL,
    archivo     VARCHAR(255),
    procesadas  INTEGER      NOT NULL DEFAULT 0,
    exitosas    INTEGER      NOT NULL DEFAULT 0,
    con_errores INTEGER      NOT NULL DEFAULT 0,
    estado      VARCHAR(20)  NOT NULL,
    detalles    TEXT,
    usuario     VARCHAR(100),
    fecha       TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_migracion_log_tenant ON migracion_log(tenant_id);
CREATE INDEX idx_migracion_log_fecha  ON migracion_log(fecha DESC);
