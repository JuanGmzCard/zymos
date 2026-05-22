-- ============================================================
-- Alera - Esquema inicial de base de datos
-- V1: Creación de todas las tablas del sistema
-- ============================================================

CREATE TABLE IF NOT EXISTS tipos_cerveza (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS usuarios (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    rol        VARCHAR(50)  NOT NULL DEFAULT 'ADMIN',
    activo     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS equipos (
    id                         BIGSERIAL PRIMARY KEY,
    nombre                     VARCHAR(255) NOT NULL,
    tipo                       VARCHAR(50)  NOT NULL,
    estado                     VARCHAR(50)  NOT NULL DEFAULT 'OPERATIVO',
    capacidad                  DECIMAL(10, 2),
    unidad_capacidad           VARCHAR(50),
    fecha_adquisicion          DATE,
    fecha_ultimo_mantenimiento DATE,
    proximo_mantenimiento      DATE,
    observaciones              TEXT,
    created_at                 TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lotes_cerveza (
    id                    BIGSERIAL PRIMARY KEY,
    codigo_lote           VARCHAR(255) NOT NULL UNIQUE,
    estilo                VARCHAR(255) NOT NULL,
    fecha_elaboracion     DATE,
    agua_utilizada        DECIMAL(10, 3),
    ph_agua               DECIMAL(5,  2),
    litros_finales        DECIMAL(10, 3),
    clarificante          VARCHAR(255),
    densidad_inicial      DECIMAL(8,  3),
    densidad_final        DECIMAL(8,  3),
    densidad_final_fecha  DATE,
    equipo_fermentador_id BIGINT REFERENCES equipos(id) ON DELETE SET NULL,
    -- Fermentación
    ferm_fecha_inicial     DATE,
    ferm_fecha_final_ideal DATE,
    ferm_temperatura       DECIMAL(5, 1),
    ferm_fecha_final       DATE,
    -- Acondicionamiento
    acond_fecha_inicial     DATE,
    acond_fecha_final_ideal DATE,
    acond_temperatura       DECIMAL(5, 1),
    acond_fecha_final       DATE,
    -- Maduración
    madur_fecha_inicial     DATE,
    madur_fecha_final_ideal DATE,
    madur_temperatura       DECIMAL(5, 1),
    madur_fecha_final       DATE,
    -- Carbonatación
    carb_fecha_inicial     DATE,
    carb_fecha_final_ideal DATE,
    carb_temperatura       DECIMAL(5, 1),
    carb_fecha_final       DATE,
    observaciones          TEXT,
    created_at             TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ingredientes (
    id       BIGSERIAL PRIMARY KEY,
    tipo     VARCHAR(50)  NOT NULL,
    nombre   VARCHAR(255) NOT NULL,
    cantidad VARCHAR(100),
    lote_id  BIGINT REFERENCES lotes_cerveza(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mantenimientos_equipo (
    id                    BIGSERIAL PRIMARY KEY,
    fecha                 DATE         NOT NULL,
    tipo                  VARCHAR(50)  NOT NULL,
    descripcion           TEXT,
    tecnico               VARCHAR(255),
    costo                 DECIMAL(12, 2),
    proximo_mantenimiento DATE,
    created_at            TIMESTAMP,
    equipo_id             BIGINT REFERENCES equipos(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS insumos_inventario (
    id                BIGSERIAL PRIMARY KEY,
    nombre            VARCHAR(255) NOT NULL,
    tipo              VARCHAR(50)  NOT NULL,
    cantidad          DECIMAL(10, 3) NOT NULL DEFAULT 0,
    unidad            VARCHAR(50),
    stock_minimo      DECIMAL(10, 3) NOT NULL DEFAULT 0,
    proveedor         VARCHAR(255),
    fecha_vencimiento DATE,
    observaciones     TEXT
);

CREATE TABLE IF NOT EXISTS facturas_proveedor (
    id               BIGSERIAL PRIMARY KEY,
    numero_factura   VARCHAR(255),
    proveedor        VARCHAR(255) NOT NULL,
    fecha_factura    DATE,
    descripcion      TEXT,
    subtotal         DECIMAL(15, 2) NOT NULL DEFAULT 0,
    porcentaje_iva   DECIMAL(5,  2) NOT NULL DEFAULT 0,
    valor_iva        DECIMAL(15, 2) NOT NULL DEFAULT 0,
    costo_envio      DECIMAL(15, 2) NOT NULL DEFAULT 0,
    valor_total      DECIMAL(15, 2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS factura_items (
    id                    BIGSERIAL PRIMARY KEY,
    tipo_item             VARCHAR(50),
    nombre                VARCHAR(255),
    tipo_insumo           VARCHAR(50),
    tipo_equipo           VARCHAR(50),
    cantidad              DECIMAL(10, 3) NOT NULL DEFAULT 1,
    unidad                VARCHAR(50),
    valor_unitario        DECIMAL(15, 2) NOT NULL DEFAULT 0,
    porcentaje_descuento  DECIMAL(5,  2) NOT NULL DEFAULT 0,
    porcentaje_iva_item   DECIMAL(5,  2) NOT NULL DEFAULT 0,
    valor_linea           DECIMAL(15, 2) NOT NULL DEFAULT 0,
    factura_id            BIGINT REFERENCES facturas_proveedor(id) ON DELETE CASCADE
);

-- Índices para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_lotes_estilo       ON lotes_cerveza(estilo);
CREATE INDEX IF NOT EXISTS idx_lotes_created_at   ON lotes_cerveza(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_lotes_fermentador  ON lotes_cerveza(equipo_fermentador_id);
CREATE INDEX IF NOT EXISTS idx_insumos_nombre     ON insumos_inventario(LOWER(nombre));
CREATE INDEX IF NOT EXISTS idx_ingredientes_lote  ON ingredientes(lote_id);
