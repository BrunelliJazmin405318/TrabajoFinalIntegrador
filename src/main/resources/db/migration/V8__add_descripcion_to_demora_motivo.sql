-- V8: asegura columna DESCRIPCION en demora_motivo

-- 1) Agregar columna si no existe
ALTER TABLE demora_motivo
    ADD COLUMN IF NOT EXISTS descripcion TEXT;

-- 2) Completar descripción para filas existentes (según código)
UPDATE demora_motivo
SET descripcion = 'Falta de repuesto'
WHERE (descripcion IS NULL OR descripcion = '')
  AND codigo = 'FALTA_REPUESTO';

UPDATE demora_motivo
SET descripcion = 'Esperando autorización del cliente'
WHERE (descripcion IS NULL OR descripcion = '')
  AND codigo = 'AUTORIZACION_CLIENTE';

UPDATE demora_motivo
SET descripcion = 'Capacidad del taller / cola de trabajo'
WHERE (descripcion IS NULL OR descripcion = '')
  AND codigo = 'CAPACIDAD_TALLER';

-- 3) Para cualquier otra fila, poner algo genérico
UPDATE demora_motivo
SET descripcion = COALESCE(descripcion, 'Motivo de demora')
WHERE descripcion IS NULL OR descripcion = '';