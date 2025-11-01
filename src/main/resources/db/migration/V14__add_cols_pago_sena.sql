ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS sena_payment_id VARCHAR(50);

ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS sena_payment_status VARCHAR(20);

ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS sena_paid_at TIMESTAMP;