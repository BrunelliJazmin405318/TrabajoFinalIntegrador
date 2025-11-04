ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS final_monto           NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS final_estado          VARCHAR(20),
    ADD COLUMN IF NOT EXISTS final_payment_id      VARCHAR(64),
    ADD COLUMN IF NOT EXISTS final_payment_status  VARCHAR(32),
    ADD COLUMN IF NOT EXISTS final_paid_at         TIMESTAMP;