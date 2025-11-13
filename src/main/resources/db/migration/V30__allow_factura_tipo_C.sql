ALTER TABLE factura_mock
DROP CONSTRAINT factura_mock_tipo_check;

ALTER TABLE factura_mock
    ADD CONSTRAINT factura_mock_tipo_check
        CHECK (tipo IN ('A', 'B', 'C'));