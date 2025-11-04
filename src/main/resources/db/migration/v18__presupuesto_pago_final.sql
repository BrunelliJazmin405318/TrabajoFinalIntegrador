-- v17__presupuesto_pago_final.sql
ALTER TABLE presupuesto
    ADD COLUMN IF NOT EXISTS final_monto          NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS final_payment_id     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS final_payment_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS final_paid_at        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS final_estado         VARCHAR(20)
    CHECK (final_estado IN ('PENDIENTE','ACREDITADA','CANCELADA'));