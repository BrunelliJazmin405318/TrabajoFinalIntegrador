-- Índices útiles para HU1/HU2

-- Ya tenés UNIQUE en nro_orden (eso crea un índice); este otro es innecesario si el UNIQUE existe.
-- Lo dejo comentado por claridad:
-- CREATE INDEX IF NOT EXISTS idx_orden_trabajo_nro_orden ON orden_trabajo(nro_orden);

-- Consultas de historial por orden y ordenadas por fecha_inicio
CREATE INDEX IF NOT EXISTS idx_historial_orden_fecha
    ON orden_etapa_historial (orden_id, fecha_inicio);

-- Filtrar por etapa (opcional, sirve para reportes y validaciones)
CREATE INDEX IF NOT EXISTS idx_historial_etapa
    ON orden_etapa_historial (etapa_codigo);

-- Pequeña guarda: tipo de unidad válido
ALTER TABLE unidad_trabajo
    ADD CONSTRAINT unidad_trabajo_tipo_chk
        CHECK (tipo IN ('MOTOR', 'TAPA'));

-- (Opcional) Evitar fechas de fin anteriores al inicio
ALTER TABLE orden_etapa_historial
    ADD CONSTRAINT historial_fechas_chk
        CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio);