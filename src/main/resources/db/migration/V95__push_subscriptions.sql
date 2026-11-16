-- Push subscriptions para notificaciones del navegador (Web Push / VAPID)
-- Sin @TenantId: las queries usan tenant_id explícito para evitar el filtro Hibernate
-- en contextos sin TenantContext (jobs, webhooks).
CREATE TABLE push_subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  VARCHAR(100) NOT NULL,
    username   VARCHAR(100) NOT NULL,
    endpoint   TEXT         NOT NULL,
    p256dh     TEXT         NOT NULL,
    auth_key   TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_push_endpoint UNIQUE (endpoint)
);

CREATE INDEX ix_push_tenant_user ON push_subscriptions(tenant_id, username);
