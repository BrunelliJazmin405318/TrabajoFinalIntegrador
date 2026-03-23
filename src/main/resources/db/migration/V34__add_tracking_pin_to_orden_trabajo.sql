-- Agrega soporte de PIN de seguimiento (hash) + protección anti brute-force

ALTER TABLE orden_trabajo
    ADD COLUMN tracking_pin_hash VARCHAR(100),
  ADD COLUMN tracking_pin_last4 VARCHAR(4),
  ADD COLUMN tracking_pin_created_at TIMESTAMP NOT NULL DEFAULT now(),
  ADD COLUMN tracking_pin_enabled BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN pin_fail_count INT NOT NULL DEFAULT 0,
  ADD COLUMN pin_last_fail_at TIMESTAMP NULL,
  ADD COLUMN pin_locked_until TIMESTAMP NULL;

-- Índices útiles
CREATE INDEX IF NOT EXISTS idx_ot_nro_orden ON orden_trabajo (nro_orden);
CREATE INDEX IF NOT EXISTS idx_ot_pin_enabled ON orden_trabajo (tracking_pin_enabled);
CREATE INDEX IF NOT EXISTS idx_ot_pin_locked_until ON orden_trabajo (pin_locked_until);