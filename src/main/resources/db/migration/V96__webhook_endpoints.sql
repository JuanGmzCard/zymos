CREATE TABLE webhook_endpoints (
    id               BIGSERIAL    PRIMARY KEY,
    tenant_id        VARCHAR(100) NOT NULL,
    nombre           VARCHAR(100) NOT NULL,
    url              VARCHAR(500) NOT NULL,
    secret           VARCHAR(100) NOT NULL,
    eventos          VARCHAR(500) NOT NULL DEFAULT 'lote.fase_cambiada',
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP,
    created_by       VARCHAR(100),
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(100)
);

CREATE INDEX idx_webhook_endpoints_tenant ON webhook_endpoints(tenant_id);
