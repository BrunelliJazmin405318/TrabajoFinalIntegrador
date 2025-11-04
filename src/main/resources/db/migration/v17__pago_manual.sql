-- v16__pago_manual.sql
CREATE TABLE IF NOT EXISTS pago_manual (
                                           id              BIGSERIAL PRIMARY KEY,
                                           presupuesto_id  BIGINT NOT NULL REFERENCES presupuesto(id),
    tipo            VARCHAR(10) NOT NULL CHECK (tipo IN ('SENA','FINAL')),
    medio           VARCHAR(30) NOT NULL, -- EFECTIVO / TRANSFERENCIA / TARJETA / OTRO
    referencia      VARCHAR(100),         -- nro recibo / op. bancaria / etc
    monto           NUMERIC(12,2) NOT NULL,
    fecha_pago      TIMESTAMP NOT NULL DEFAULT now(),
    usuario         VARCHAR(80),
    nota            TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
    );

-- Índices útiles
CREATE INDEX IF NOT EXISTS ix_pago_manual_presupuesto ON pago_manual(presupuesto_id);
CREATE INDEX IF NOT EXISTS ix_pago_manual_tipo ON pago_manual(tipo);