CREATE TABLE clientes (
    id                 BIGSERIAL     PRIMARY KEY,
    tenant_id          VARCHAR(100)  NOT NULL DEFAULT 'default',
    nombre             VARCHAR(200)  NOT NULL,
    razon_social       VARCHAR(200),
    nit                VARCHAR(50),
    regimen_tributario VARCHAR(30),
    email              VARCHAR(200),
    telefono           VARCHAR(50),
    direccion_despacho VARCHAR(300),
    ciudad             VARCHAR(100),
    departamento       VARCHAR(100),
    lista_precio       VARCHAR(30),
    activo             BOOLEAN       NOT NULL DEFAULT TRUE,
    notas              VARCHAR(500),
    created_at         TIMESTAMP,
    created_by         VARCHAR(100),
    last_modified_at   TIMESTAMP,
    last_modified_by   VARCHAR(100)
);

-- NIT único por tenant solo cuando no es null
CREATE UNIQUE INDEX idx_clientes_nit_tenant
    ON clientes (tenant_id, nit)
    WHERE nit IS NOT NULL;

CREATE INDEX idx_clientes_tenant        ON clientes (tenant_id);
CREATE INDEX idx_clientes_tenant_nombre ON clientes (tenant_id, LOWER(nombre));
CREATE INDEX idx_clientes_activo        ON clientes (tenant_id, activo);
