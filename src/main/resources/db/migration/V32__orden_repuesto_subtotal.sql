ALTER TABLE orden_repuesto
    ADD COLUMN IF NOT EXISTS subtotal NUMERIC(12,2);

UPDATE orden_repuesto
SET subtotal = precio_unit * cantidad
WHERE subtotal IS NULL;

ALTER TABLE orden_repuesto
    ALTER COLUMN subtotal SET NOT NULL;