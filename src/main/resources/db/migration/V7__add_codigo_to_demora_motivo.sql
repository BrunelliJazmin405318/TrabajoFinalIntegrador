-- V7: asegura columna CODIGO en demora_motivo y la deja consistente

-- 1) Agregar columna si no existe
ALTER TABLE demora_motivo
    ADD COLUMN IF NOT EXISTS codigo TEXT;

-- 2) Completar codigo para filas existentes (mapa básico por nombres viejos)
UPDATE demora_motivo
SET codigo = 'FALTA_REPUESTO'
WHERE codigo IS NULL AND nombre ILIKE '%REPUEST%';

UPDATE demora_motivo
SET codigo = 'AUTORIZACION_CLIENTE'
WHERE codigo IS NULL AND (nombre ILIKE '%CLIENTE%' OR nombre ILIKE '%AUTORIZ%');

UPDATE demora_motivo
SET codigo = 'CAPACIDAD_TALLER'
WHERE codigo IS NULL AND (nombre ILIKE '%CAPACIDAD%' OR nombre ILIKE '%TALLER%');

-- 3) Para cualquier otra fila que haya quedado sin codigo, generamos uno
UPDATE demora_motivo
SET codigo = REGEXP_REPLACE(UPPER(COALESCE(nombre,'')), '[^A-Z0-9]+', '_', 'g')
WHERE codigo IS NULL OR codigo = '';

-- 4) Asegurar unicidad de codigo
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'demora_motivo'
          AND constraint_name = 'uq_demora_motivo_codigo'
    ) THEN
ALTER TABLE demora_motivo
    ADD CONSTRAINT uq_demora_motivo_codigo UNIQUE (codigo);
END IF;
END$$;

-- 5) (Opcional) mantener la columna nombre por compatibilidad. No la borramos.