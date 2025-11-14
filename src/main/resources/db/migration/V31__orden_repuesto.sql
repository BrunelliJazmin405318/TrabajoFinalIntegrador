-- Repuestos asociados a la Orden de Trabajo

CREATE TABLE IF NOT EXISTS orden_repuesto (
                                              id           BIGSERIAL PRIMARY KEY,
                                              orden_id     BIGINT NOT NULL REFERENCES orden_trabajo(id) ON DELETE CASCADE,
    descripcion  VARCHAR(200) NOT NULL,
    cantidad     NUMERIC(10,2) NOT NULL DEFAULT 1,
    precio_unit  NUMERIC(12,2) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    created_by   VARCHAR(80)
    );

-- Índices
CREATE INDEX IF NOT EXISTS idx_orden_repuesto_orden
    ON orden_repuesto(orden_id);