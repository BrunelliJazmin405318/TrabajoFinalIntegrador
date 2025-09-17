INSERT INTO cliente (nombre, telefono) VALUES ('Juan Pérez','351-5551234');

INSERT INTO unidad_trabajo (cliente_id, tipo, marca, modelo, nro_motor)
VALUES (1, 'MOTOR', 'Fiat', '1.4 Fire', 'NF12345');

INSERT INTO orden_trabajo (nro_orden, unidad_id, estado_actual)
VALUES ('OT-0001', 1, 'DESPIECE_LAVADO');

INSERT INTO orden_etapa_historial (orden_id, etapa_codigo, fecha_inicio, fecha_fin, observacion, usuario)
VALUES
    (1,'INGRESO', now() - interval '2 days', now() - interval '1 days', 'Ingreso a taller', 'admin'),
    (1,'DESPIECE_LAVADO', now() - interval '1 days', null, 'Lavado en curso', 'admin');