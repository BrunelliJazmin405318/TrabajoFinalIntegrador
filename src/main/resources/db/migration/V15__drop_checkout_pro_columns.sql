-- Eliminamos columnas usadas solo por Checkout PRO
ALTER TABLE presupuesto
DROP COLUMN IF EXISTS sena_preference_id,
DROP COLUMN IF EXISTS sena_init_point;