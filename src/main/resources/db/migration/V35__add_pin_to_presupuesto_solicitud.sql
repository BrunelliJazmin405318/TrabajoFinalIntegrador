ALTER TABLE presupuesto_solicitud
    ADD COLUMN tracking_pin_hash VARCHAR(100),
  ADD COLUMN tracking_pin_last4 VARCHAR(4),
  ADD COLUMN tracking_pin_created_at TIMESTAMP NOT NULL DEFAULT now(),
  ADD COLUMN tracking_pin_expires_at TIMESTAMP NULL,
  ADD COLUMN tracking_pin_enabled BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN pin_fail_count INT NOT NULL DEFAULT 0,
  ADD COLUMN pin_last_fail_at TIMESTAMP NULL,
  ADD COLUMN pin_locked_until TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_ps_pin_enabled       ON presupuesto_solicitud (tracking_pin_enabled);
CREATE INDEX IF NOT EXISTS idx_ps_pin_expires_at    ON presupuesto_solicitud (tracking_pin_expires_at);
CREATE INDEX IF NOT EXISTS idx_ps_pin_locked_until  ON presupuesto_solicitud (pin_locked_until);