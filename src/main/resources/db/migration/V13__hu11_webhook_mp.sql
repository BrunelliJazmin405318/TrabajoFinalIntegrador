ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS sena_payment_id      VARCHAR(40),
    ADD COLUMN IF NOT EXISTS sena_payment_status  VARCHAR(30),
    ADD COLUMN IF NOT EXISTS sena_paid_at         TIMESTAMP;