-- ===========================
-- V11__tarifas_tapa.sql
-- ===========================
-- 💡 Tarifas base para TAPA

INSERT INTO servicio_tarifa (nombre_servicio, vehiculo_tipo, precio) VALUES
-- 🔩 TAPA - CONVENCIONAL
('Planeado de tapa','CONVENCIONAL',25000),
('Rectificación de asientos de válvulas','CONVENCIONAL',30000),
('Cambio de guías de válvula','CONVENCIONAL',22000),
('Cambio de retenes','CONVENCIONAL',12000),
('Prueba hidráulica de tapa','CONVENCIONAL',18000),
('Ajuste de resortes y balancines','CONVENCIONAL',15000),
('Limpieza y arenado de tapa','CONVENCIONAL',10000),

-- 🔩 TAPA - IMPORTADO
('Planeado de tapa','IMPORTADO',35000),
('Rectificación de asientos de válvulas','IMPORTADO',42000),
('Cambio de guías de válvula','IMPORTADO',32000),
('Cambio de retenes','IMPORTADO',16000),
('Prueba hidráulica de tapa','IMPORTADO',26000),
('Ajuste de resortes y balancines','IMPORTADO',21000),
('Limpieza y arenado de tapa','IMPORTADO',14000);

-- ===========================
-- Fin de V11__tarifas_tapa.sql
-- ===========================