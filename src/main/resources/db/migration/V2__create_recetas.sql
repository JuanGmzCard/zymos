-- ============================================================
-- Alera - Recetas reutilizables
-- V2: tablas recetas, receta_ingredientes, escalones_macerado
-- ============================================================

CREATE TABLE IF NOT EXISTS recetas (
    id                     BIGSERIAL PRIMARY KEY,
    nombre                 VARCHAR(150) NOT NULL UNIQUE,
    estilo                 VARCHAR(100) NOT NULL,
    descripcion            TEXT,
    activa                 BOOLEAN NOT NULL DEFAULT TRUE,
    agua_macerado          DECIMAL(10, 3),
    unidad_agua_macerado   VARCHAR(10),
    agua_sparge            DECIMAL(10, 3),
    unidad_agua_sparge     VARCHAR(10),
    tiempo_hervor_minutos  INTEGER,
    og_objetivo            DECIMAL(8, 4),
    fg_objetivo            DECIMAL(8, 4),
    notas                  TEXT,
    created_at             TIMESTAMP
);

CREATE TABLE IF NOT EXISTS receta_ingredientes (
    id         BIGSERIAL PRIMARY KEY,
    receta_id  BIGINT      NOT NULL REFERENCES recetas(id) ON DELETE CASCADE,
    tipo       VARCHAR(20) NOT NULL,
    nombre     VARCHAR(200) NOT NULL,
    cantidad   VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS escalones_macerado (
    id                 BIGSERIAL PRIMARY KEY,
    receta_id          BIGINT       NOT NULL REFERENCES recetas(id) ON DELETE CASCADE,
    nombre             VARCHAR(100) NOT NULL,
    duracion_minutos   INTEGER,
    temperatura_c      DECIMAL(5, 2),
    orden              INTEGER NOT NULL DEFAULT 0
);
