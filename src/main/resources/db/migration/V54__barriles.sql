-- V54: Keg/barril tracking
-- Tabla principal de barriles
CREATE TABLE barriles (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           VARCHAR(100) NOT NULL DEFAULT 'default',
    codigo              VARCHAR(50)  NOT NULL,
    tipo                VARCHAR(50),
    capacidad_litros    DECIMAL(8,2),
    estado              VARCHAR(20)  NOT NULL DEFAULT 'DISPONIBLE',
    lote_id             BIGINT       REFERENCES lotes_cerveza(id) ON DELETE SET NULL,
    codigo_lote         VARCHAR(50),
    cliente_nombre      VARCHAR(200),
    fecha_despacho      DATE,
    observaciones       VARCHAR(500),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    last_modified_at    TIMESTAMP,
    last_modified_by    VARCHAR(100),
    CONSTRAINT barriles_estado_check CHECK (estado IN (
        'DISPONIBLE','LLENO','DESPACHADO','VACIO','LIMPIEZA','BAJA'
    ))
);

CREATE UNIQUE INDEX ux_barriles_codigo_tenant ON barriles (tenant_id, codigo);
CREATE INDEX idx_barriles_tenant              ON barriles (tenant_id);
CREATE INDEX idx_barriles_estado              ON barriles (tenant_id, estado);
CREATE INDEX idx_barriles_lote                ON barriles (lote_id) WHERE lote_id IS NOT NULL;

-- Historial de movimientos de estado (sin FK para preservar historia)
CREATE TABLE movimientos_barriles (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        VARCHAR(100) NOT NULL DEFAULT 'default',
    barril_id        BIGINT       NOT NULL,
    estado_anterior  VARCHAR(20),
    estado_nuevo     VARCHAR(20)  NOT NULL,
    usuario          VARCHAR(100),
    notas            VARCHAR(500),
    fecha            TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mov_barriles_barril ON movimientos_barriles (barril_id);
CREATE INDEX idx_mov_barriles_tenant ON movimientos_barriles (tenant_id);
