-- BPM-REG-01: Registro de Síntomas / Estado de Salud
CREATE TABLE bpm_registros_sintomas (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    fecha           DATE NOT NULL,
    nombre_manipulador VARCHAR(200) NOT NULL,
    diarrea         BOOLEAN NOT NULL DEFAULT FALSE,
    vomito          BOOLEAN NOT NULL DEFAULT FALSE,
    fiebre          BOOLEAN NOT NULL DEFAULT FALSE,
    infeccion_respiratoria BOOLEAN NOT NULL DEFAULT FALSE,
    lesion_piel     BOOLEAN NOT NULL DEFAULT FALSE,
    observaciones   TEXT,
    firma_manipulador VARCHAR(200),
    firma_responsable VARCHAR(200),
    created_at      TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_bpm_sint_tenant_fecha ON bpm_registros_sintomas(tenant_id, fecha);

-- BPM-L+D-02: Control Preparación Soluciones Desinfectantes
CREATE TABLE bpm_soluciones_desinfectantes (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    fecha           DATE NOT NULL,
    hora            TIME,
    producto        VARCHAR(200) NOT NULL,
    cantidad_agua   DECIMAL(10,3),
    unidad_agua     VARCHAR(10)  DEFAULT 'L',
    cantidad_producto DECIMAL(10,3),
    unidad_producto VARCHAR(10)  DEFAULT 'mL',
    concentracion_final DECIMAL(10,2),
    responsable     VARCHAR(200),
    created_at      TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_bpm_sol_tenant_fecha ON bpm_soluciones_desinfectantes(tenant_id, fecha);

-- BPM-CP-01: Control Avistamiento de Plagas
CREATE TABLE bpm_avistamiento_plagas (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    fecha           DATE NOT NULL,
    presencia_plagas BOOLEAN NOT NULL DEFAULT FALSE,
    tipo_plagas     VARCHAR(200),
    estado_ventanas_puertas VARCHAR(10) DEFAULT 'OK',
    accion_tomada   TEXT,
    firma           VARCHAR(200),
    created_at      TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_bpm_plagas_tenant_fecha ON bpm_avistamiento_plagas(tenant_id, fecha);

-- BPM-ER-01: Evacuación de Residuos
CREATE TABLE bpm_evacuacion_residuos (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    fecha           DATE NOT NULL,
    hora_salida     TIME,
    tipo_residuo    VARCHAR(50) NOT NULL,
    recipientes_limpios BOOLEAN NOT NULL DEFAULT FALSE,
    responsable     VARCHAR(200),
    created_at      TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_bpm_resid_tenant_fecha ON bpm_evacuacion_residuos(tenant_id, fecha);

-- BPM-L+D-01: Registro Limpieza y Desinfección de Áreas y Utensilios
CREATE TABLE bpm_limpieza_desinfeccion (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(100) NOT NULL,
    fecha           DATE NOT NULL,
    dia             VARCHAR(20),
    area_utensilio  VARCHAR(200) NOT NULL,
    detergente_usado VARCHAR(200),
    sanitizador_usado VARCHAR(200),
    concentracion   VARCHAR(100),
    responsable     VARCHAR(200),
    visto_bueno     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_bpm_limp_tenant_fecha ON bpm_limpieza_desinfeccion(tenant_id, fecha);
