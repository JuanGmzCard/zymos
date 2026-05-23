CREATE TABLE historial_tenants (
    id        BIGSERIAL PRIMARY KEY,
    subdomain VARCHAR(100) NOT NULL,
    accion    VARCHAR(50)  NOT NULL,
    usuario   VARCHAR(100),
    fecha     TIMESTAMP    NOT NULL DEFAULT NOW(),
    detalles  VARCHAR(500)
);

CREATE INDEX idx_historial_tenants_subdomain ON historial_tenants(subdomain);
CREATE INDEX idx_historial_tenants_fecha     ON historial_tenants(fecha DESC);
