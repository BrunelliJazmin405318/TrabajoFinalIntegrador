CREATE TABLE cliente (
                         id BIGSERIAL PRIMARY KEY,
                         nombre VARCHAR(120) NOT NULL,
                         telefono VARCHAR(40)
);

CREATE TABLE unidad_trabajo (
                                id BIGSERIAL PRIMARY KEY,
                                cliente_id BIGINT NOT NULL REFERENCES cliente(id),
                                tipo VARCHAR(16) NOT NULL,
                                marca VARCHAR(80),
                                modelo VARCHAR(80),
                                nro_motor VARCHAR(80)
);

CREATE TABLE orden_trabajo (
                               id BIGSERIAL PRIMARY KEY,
                               nro_orden VARCHAR(30) UNIQUE NOT NULL,
                               unidad_id BIGINT NOT NULL REFERENCES unidad_trabajo(id),
                               estado_actual VARCHAR(40) NOT NULL,
                               garantia_desde DATE,
                               garantia_hasta DATE,
                               creada_en TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE etapa_catalogo (
                                codigo VARCHAR(40) PRIMARY KEY,
                                orden INT NOT NULL
);
INSERT INTO etapa_catalogo (codigo, orden) VALUES
                                               ('INGRESO',1),('DESPIECE_LAVADO',2),('DIAGNOSTICO',3),('MAQUINADO',4),
                                               ('SEMI_ARMADO',5),('CONTROL_CALIDAD',6),('LISTO_RETIRAR',7),('ENTREGADO',8);

CREATE TABLE demora_motivo (
                               id BIGSERIAL PRIMARY KEY,
                               nombre VARCHAR(100) NOT NULL
);
INSERT INTO demora_motivo (nombre) VALUES
                                       ('ESPERANDO REPUESTO'),('CLIENTE NO AUTORIZA'),('OTROS');

CREATE TABLE orden_etapa_historial (
                                       id BIGSERIAL PRIMARY KEY,
                                       orden_id BIGINT NOT NULL REFERENCES orden_trabajo(id),
                                       etapa_codigo VARCHAR(40) NOT NULL REFERENCES etapa_catalogo(codigo),
                                       fecha_inicio TIMESTAMP NOT NULL DEFAULT now(),
                                       fecha_fin TIMESTAMP,
                                       observacion TEXT,
                                       demora_motivo_id BIGINT REFERENCES demora_motivo(id),
                                       usuario VARCHAR(80)
);

CREATE TABLE usuario (
                         id BIGSERIAL PRIMARY KEY,
                         username VARCHAR(60) UNIQUE NOT NULL,
                         password VARCHAR(200) NOT NULL,
                         rol VARCHAR(20) NOT NULL
);