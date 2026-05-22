CREATE TABLE adiciones_hervor (
    id               BIGSERIAL PRIMARY KEY,
    receta_id        BIGINT       NOT NULL REFERENCES recetas(id) ON DELETE CASCADE,
    nombre           VARCHAR(150) NOT NULL,
    minutos_restantes INTEGER,
    cantidad         DECIMAL(10, 3),
    unidad           VARCHAR(20),
    orden            INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_adiciones_hervor_receta ON adiciones_hervor(receta_id);