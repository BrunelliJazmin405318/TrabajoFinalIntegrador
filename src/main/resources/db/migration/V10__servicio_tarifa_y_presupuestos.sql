-- ===========================
-- V10__presupuesto.sql
-- ===========================

-- 🧩 Tabla principal de presupuestos
CREATE TABLE IF NOT EXISTS presupuesto (
                                           id BIGSERIAL PRIMARY KEY,
                                           solicitud_id BIGINT NOT NULL,
                                           cliente_nombre VARCHAR(120) NOT NULL,
    cliente_email VARCHAR(120),
    vehiculo_tipo VARCHAR(16) NOT NULL, -- CONVENCIONAL | IMPORTADO
    total NUMERIC(12,2) NOT NULL DEFAULT 0,
    estado VARCHAR(16) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE | APROBADO | RECHAZADO
    creada_en TIMESTAMP NOT NULL DEFAULT now(),
    decision_usuario VARCHAR(80),
    decision_fecha TIMESTAMP,
    decision_motivo TEXT
    );

-- Restricción de estado válido
ALTER TABLE presupuesto
    ADD CONSTRAINT presupuesto_estado_chk
        CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO'));

CREATE INDEX IF NOT EXISTS idx_presupuesto_estado ON presupuesto(estado);
CREATE INDEX IF NOT EXISTS idx_presupuesto_creada ON presupuesto(creada_en);

-- 🧾 Tabla de ítems del presupuesto (detalle de servicios)
CREATE TABLE IF NOT EXISTS presupuesto_item (
                                                id BIGSERIAL PRIMARY KEY,
                                                presupuesto_id BIGINT NOT NULL REFERENCES presupuesto(id) ON DELETE CASCADE,
    servicio_nombre VARCHAR(120) NOT NULL,
    precio_unitario NUMERIC(12,2) NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_presupuesto_item_presupuesto ON presupuesto_item(presupuesto_id);

-- 💰 Tabla de tarifas base de servicios
CREATE TABLE IF NOT EXISTS servicio_tarifa (
                                               id BIGSERIAL PRIMARY KEY,
                                               nombre_servicio VARCHAR(120) NOT NULL,
    vehiculo_tipo VARCHAR(16) NOT NULL, -- CONVENCIONAL | IMPORTADO
    precio NUMERIC(12,2) NOT NULL
    );

ALTER TABLE servicio_tarifa
    ADD CONSTRAINT servicio_tarifa_tipo_chk
        CHECK (vehiculo_tipo IN ('CONVENCIONAL','IMPORTADO'));

CREATE INDEX IF NOT EXISTS idx_servicio_tarifa_tipo ON servicio_tarifa(vehiculo_tipo);
CREATE INDEX IF NOT EXISTS idx_servicio_tarifa_nombre ON servicio_tarifa(nombre_servicio);

-- ===========================
-- Datos iniciales de tarifas base
-- ===========================

INSERT INTO servicio_tarifa (nombre_servicio, vehiculo_tipo, precio) VALUES
-- 🔧 Motor / Block (Convencional)
('Rectificación de cilindros', 'CONVENCIONAL', 50000),
('Bruñido de cilindros', 'CONVENCIONAL', 35000),
('Planeado de block', 'CONVENCIONAL', 30000),
('Cambio de camisas', 'CONVENCIONAL', 40000),

-- ⚙️ Motor / Conjunto móvil (Convencional)
('Rectificación de cigüeñal', 'CONVENCIONAL', 45000),
('Pulido de cigüeñal', 'CONVENCIONAL', 20000),
('Rectificación de bielas', 'CONVENCIONAL', 30000),

-- 🧽 General / Ensamble (Convencional)
('Limpieza de block y piezas', 'CONVENCIONAL', 15000),
('Armado de motor completo', 'CONVENCIONAL', 60000),

-- 🔧 Motor / Block (Importado)
('Rectificación de cilindros', 'IMPORTADO', 70000),
('Bruñido de cilindros', 'IMPORTADO', 50000),
('Planeado de block', 'IMPORTADO', 45000),
('Cambio de camisas', 'IMPORTADO', 60000),

-- ⚙️ Motor / Conjunto móvil (Importado)
('Rectificación de cigüeñal', 'IMPORTADO', 65000),
('Pulido de cigüeñal', 'IMPORTADO', 30000),
('Rectificación de bielas', 'IMPORTADO', 45000),

-- 🧽 General / Ensamble (Importado)
('Limpieza de block y piezas', 'IMPORTADO', 20000),
('Armado de motor completo', 'IMPORTADO', 85000);

-- ===========================
-- Fin de V10__presupuesto.sql
-- ===========================
