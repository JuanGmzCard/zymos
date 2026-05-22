-- V17: Registro de lecturas periódicas de fermentación por lote
-- Permite trazar la curva de densidad y temperatura a lo largo del proceso

CREATE TABLE lecturas_fermentacion (
    id          BIGSERIAL PRIMARY KEY,
    lote_id     BIGINT       NOT NULL REFERENCES lotes_cerveza(id) ON DELETE CASCADE,
    fecha       DATE         NOT NULL,
    densidad    INTEGER,                          -- formato XXXX (ej: 1042). NULL si solo se registra temperatura.
    temperatura DECIMAL(5,2),                     -- °C. NULL si solo se registra densidad.
    notas       VARCHAR(500),
    tenant_id   VARCHAR(100) NOT NULL DEFAULT 'default'
);

CREATE INDEX idx_lecturas_lote_id   ON lecturas_fermentacion(lote_id);
CREATE INDEX idx_lecturas_tenant_id ON lecturas_fermentacion(tenant_id);