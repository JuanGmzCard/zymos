CREATE TABLE contactos_whatsapp (
    id               BIGSERIAL    PRIMARY KEY,
    tenant_id        VARCHAR(100) NOT NULL,
    nombre           VARCHAR(150) NOT NULL,
    telefono         VARCHAR(30)  NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP,
    created_by       VARCHAR(100),
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(100)
);
CREATE INDEX idx_cwa_tenant ON contactos_whatsapp(tenant_id);

CREATE TABLE contactos_whatsapp_suscripciones (
    id          BIGSERIAL    PRIMARY KEY,
    contacto_id BIGINT       NOT NULL REFERENCES contactos_whatsapp(id) ON DELETE CASCADE,
    tipo        VARCHAR(50)  NOT NULL,
    tenant_id   VARCHAR(100) NOT NULL
);
CREATE INDEX idx_cwas_contacto    ON contactos_whatsapp_suscripciones(contacto_id);
CREATE INDEX idx_cwas_tipo_tenant ON contactos_whatsapp_suscripciones(tipo, tenant_id);
