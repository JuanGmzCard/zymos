-- Historial de movimientos de inventario
CREATE TABLE movimientos_inventario (
    id                 BIGSERIAL     PRIMARY KEY,
    tenant_id          VARCHAR(100)  NOT NULL DEFAULT 'default',
    insumo_id          BIGINT        NOT NULL,
    insumo_nombre      VARCHAR(200)  NOT NULL,
    tipo               VARCHAR(30)   NOT NULL,
    cantidad           DECIMAL(10,3) NOT NULL,
    cantidad_anterior  DECIMAL(10,3),
    cantidad_posterior DECIMAL(10,3),
    motivo             VARCHAR(300),
    referencia         VARCHAR(100),
    usuario            VARCHAR(100),
    fecha              TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mov_invent_insumo  ON movimientos_inventario(insumo_id, tenant_id);
CREATE INDEX idx_mov_invent_tenant  ON movimientos_inventario(tenant_id, fecha DESC);

-- Versionado de recetas
ALTER TABLE recetas ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
