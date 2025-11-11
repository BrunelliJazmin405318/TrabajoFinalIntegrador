ALTER TABLE orden_trabajo
    ADD COLUMN IF NOT EXISTS cliente_email VARCHAR(200);
