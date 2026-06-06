-- V55: Módulo de Órdenes de Compra
CREATE TABLE ordenes_compra (
    id               BIGSERIAL    PRIMARY KEY,
    tenant_id        VARCHAR(100) NOT NULL DEFAULT 'default',
    numero_oc        VARCHAR(20),
    proveedor        VARCHAR(200),
    proveedor_id     BIGINT       REFERENCES proveedores(id) ON DELETE SET NULL,
    fecha_emision    DATE         NOT NULL,
    fecha_requerida  DATE,
    estado           VARCHAR(30)  NOT NULL DEFAULT 'BORRADOR',
    notas            VARCHAR(500),
    factura_id       BIGINT,
    created_at       TIMESTAMP,
    created_by       VARCHAR(100),
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(100),
    CONSTRAINT oc_estado_check CHECK (estado IN (
        'BORRADOR','ENVIADA','RECIBIDA_PARCIAL','RECIBIDA','CANCELADA'
    ))
);

CREATE INDEX idx_oc_tenant ON ordenes_compra (tenant_id);
CREATE INDEX idx_oc_estado ON ordenes_compra (tenant_id, estado);
CREATE INDEX idx_oc_numero ON ordenes_compra (tenant_id, numero_oc);

CREATE TABLE orden_compra_items (
    id                       BIGSERIAL    PRIMARY KEY,
    tenant_id                VARCHAR(100) NOT NULL DEFAULT 'default',
    orden_id                 BIGINT       NOT NULL REFERENCES ordenes_compra(id) ON DELETE CASCADE,
    tipo_item                VARCHAR(20),
    nombre                   VARCHAR(200) NOT NULL,
    descripcion              VARCHAR(300),
    cantidad                 DECIMAL(10,3) NOT NULL CHECK (cantidad > 0),
    unidad                   VARCHAR(50),
    precio_unitario_estimado DECIMAL(12,2),
    porcentaje_iva_item      DECIMAL(5,2) DEFAULT 0,
    tipo_insumo              VARCHAR(100),
    tipo_equipo              VARCHAR(100)
);

CREATE INDEX idx_oc_items_orden  ON orden_compra_items (orden_id);
CREATE INDEX idx_oc_items_tenant ON orden_compra_items (tenant_id);
