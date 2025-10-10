-- ===========================
-- HU8: Tarifas y Presupuestos
-- ===========================

-- Tarifas por tipo de vehículo (CONVENCIONAL/IMPORTADO)
CREATE TABLE IF NOT EXISTS servicio_tarifa (
                                               id              BIGSERIAL PRIMARY KEY,
                                               nombre_servicio VARCHAR(80)  NOT NULL,
    vehiculo_tipo   VARCHAR(16)  NOT NULL,  -- CONVENCIONAL | IMPORTADO
    precio          NUMERIC(12,2) NOT NULL
    );

ALTER TABLE servicio_tarifa
    ADD CONSTRAINT servicio_tarifa_vehiculo_tipo_chk
        CHECK (vehiculo_tipo IN ('CONVENCIONAL','IMPORTADO'));

ALTER TABLE servicio_tarifa
    ADD CONSTRAINT servicio_tarifa_unq UNIQUE (nombre_servicio, vehiculo_tipo);

CREATE INDEX IF NOT EXISTS idx_tarifa_vehiculo_tipo ON servicio_tarifa(vehiculo_tipo);
CREATE INDEX IF NOT EXISTS idx_tarifa_nombre ON servicio_tarifa(nombre_servicio);

-- Semillas mínimas
INSERT INTO servicio_tarifa (nombre_servicio, vehiculo_tipo, precio) VALUES
                                                                         ('RECTIFICADO', 'CONVENCIONAL', 80000),
                                                                         ('PLANIFICADO', 'CONVENCIONAL', 30000),
                                                                         ('ENSAYO',      'CONVENCIONAL', 15000),
                                                                         ('ARMADO',      'CONVENCIONAL', 25000)
    ON CONFLICT DO NOTHING;

INSERT INTO servicio_tarifa (nombre_servicio, vehiculo_tipo, precio) VALUES
                                                                         ('RECTIFICADO', 'IMPORTADO', 110000),
                                                                         ('PLANIFICADO', 'IMPORTADO',  45000),
                                                                         ('ENSAYO',      'IMPORTADO',  22000),
                                                                         ('ARMADO',      'IMPORTADO',  38000)
    ON CONFLICT DO NOTHING;

-- Encabezado de presupuesto
CREATE TABLE IF NOT EXISTS presupuesto (
                                           id               BIGSERIAL PRIMARY KEY,
                                           solicitud_id     BIGINT REFERENCES presupuesto_solicitud(id),
    cliente_nombre   VARCHAR(120) NOT NULL,
    cliente_email    VARCHAR(120),
    vehiculo_tipo    VARCHAR(16)  NOT NULL, -- CONVENCIONAL | IMPORTADO
    total            NUMERIC(12,2) NOT NULL,
    estado           VARCHAR(16)  NOT NULL DEFAULT 'EN_REVISION', -- EN_REVISION | APROBADO | RECHAZADO
    creada_en        TIMESTAMP    NOT NULL DEFAULT now(),
    decision_usuario VARCHAR(80),
    decision_fecha   TIMESTAMP,
    decision_motivo  TEXT
    );

ALTER TABLE presupuesto
    ADD CONSTRAINT presupuesto_vehiculo_tipo_chk
        CHECK (vehiculo_tipo IN ('CONVENCIONAL','IMPORTADO'));

ALTER TABLE presupuesto
    ADD CONSTRAINT presupuesto_estado_chk
        CHECK (estado IN ('EN_REVISION','APROBADO','RECHAZADO'));

CREATE INDEX IF NOT EXISTS idx_presupuesto_estado ON presupuesto(estado);
CREATE INDEX IF NOT EXISTS idx_presupuesto_creada ON presupuesto(creada_en);
CREATE INDEX IF NOT EXISTS idx_presupuesto_solicitud ON presupuesto(solicitud_id);

-- Ítems del presupuesto
CREATE TABLE IF NOT EXISTS presupuesto_item (
                                                id               BIGSERIAL PRIMARY KEY,
                                                presupuesto_id   BIGINT NOT NULL REFERENCES presupuesto(id) ON DELETE CASCADE,
    servicio_nombre  VARCHAR(80) NOT NULL,
    precio_unitario  NUMERIC(12,2) NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_item_presupuesto ON presupuesto_item(presupuesto_id);
