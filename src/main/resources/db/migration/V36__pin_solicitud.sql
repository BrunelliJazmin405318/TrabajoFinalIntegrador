ALTER TABLE presupuesto_solicitud
    ADD COLUMN pin_hash VARCHAR(100),
  ADD COLUMN pin_expira_en TIMESTAMP,
  ADD COLUMN pin_intentos INT NOT NULL DEFAULT 0,
  ADD COLUMN pin_bloqueado_hasta TIMESTAMP;

CREATE INDEX IF NOT EXISTS ix_presupuesto_solicitud_pin_expira
    ON presupuesto_solicitud (pin_expira_en);