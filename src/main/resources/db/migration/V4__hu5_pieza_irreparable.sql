-- V4__hu5_pieza_irreparable.sql
-- Agrega la etapa especial de pieza irreparable (la ponemos al final de la secuencia)

INSERT INTO etapa_catalogo (codigo, orden)
VALUES ('PIEZA_IRREPARABLE', 99)
    ON CONFLICT (codigo) DO NOTHING;