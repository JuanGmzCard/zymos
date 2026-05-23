CREATE TABLE notificaciones (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    tipo        VARCHAR(50)  NOT NULL,
    titulo      VARCHAR(200) NOT NULL,
    mensaje     VARCHAR(500),
    url_accion  VARCHAR(300),
    leida       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notificaciones_tenant_leida ON notificaciones(tenant_id, leida);
CREATE INDEX idx_notificaciones_tenant_fecha ON notificaciones(tenant_id, created_at DESC);
