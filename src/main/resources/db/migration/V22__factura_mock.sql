
CREATE TABLE IF NOT EXISTS factura_mock (
                                            id              BIGSERIAL PRIMARY KEY,
                                            presupuesto_id  BIGINT NOT NULL REFERENCES presupuesto(id) ON DELETE CASCADE,
    tipo            VARCHAR(2) NOT NULL CHECK (tipo IN ('A','B')),
    numero          VARCHAR(20) NOT NULL,
    fecha_emision   TIMESTAMP NOT NULL DEFAULT now(),
    total           NUMERIC(12,2) NOT NULL,
    cliente_nombre  VARCHAR(120),
    cliente_email   VARCHAR(120),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_factura_mock_presupuesto ON factura_mock(presupuesto_id);