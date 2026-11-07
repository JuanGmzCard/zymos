-- Tabla de checkouts pendientes para el flujo de compra de planes SaaS
-- No usa tenant_id: es una tabla de sistema, no de tenant.
CREATE TABLE checkout_pendiente (
    id                VARCHAR(36)  PRIMARY KEY,
    subdominio        VARCHAR(100) NOT NULL,
    email             VARCHAR(200) NOT NULL,
    nombre_cerveceria VARCHAR(200) NOT NULL,
    plan_nivel        VARCHAR(20)  NOT NULL,
    mp_preference_id  VARCHAR(200),
    mp_payment_id     VARCHAR(200),
    estado            VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    procesado_at      TIMESTAMP
);

CREATE INDEX idx_checkout_subdominio ON checkout_pendiente (subdominio);
CREATE INDEX idx_checkout_mp_payment ON checkout_pendiente (mp_payment_id);
CREATE INDEX idx_checkout_estado     ON checkout_pendiente (estado);
