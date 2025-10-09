-- HU7: Solicitud de presupuesto

CREATE TABLE IF NOT EXISTS presupuesto_solicitud (
                                                     id               BIGSERIAL PRIMARY KEY,
                                                     cliente_nombre   VARCHAR(120) NOT NULL,
    cliente_telefono VARCHAR(40),
    cliente_email    VARCHAR(120),
    tipo_unidad      VARCHAR(16)  NOT NULL, -- MOTOR | TAPA
    marca            VARCHAR(80),
    modelo           VARCHAR(80),
    nro_motor        VARCHAR(80),
    descripcion      TEXT,

    estado           VARCHAR(16)  NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE | APROBADO | RECHAZADO
    creada_en        TIMESTAMP    NOT NULL DEFAULT now(),

    decision_usuario VARCHAR(80),
    decision_fecha   TIMESTAMP,
    decision_motivo  TEXT
    );

-- Guardas
ALTER TABLE presupuesto_solicitud
    ADD CONSTRAINT presupuesto_tipo_unidad_chk
        CHECK (tipo_unidad IN ('MOTOR','TAPA'));

ALTER TABLE presupuesto_solicitud
    ADD CONSTRAINT presupuesto_estado_chk
        CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO'));

CREATE INDEX IF NOT EXISTS idx_presupuesto_estado ON presupuesto_solicitud(estado);
CREATE INDEX IF NOT EXISTS idx_presupuesto_creada ON presupuesto_solicitud(creada_en);