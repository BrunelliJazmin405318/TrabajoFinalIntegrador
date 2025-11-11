CREATE TABLE IF NOT EXISTS notification (
                                            id             BIGSERIAL PRIMARY KEY,
                                            type           VARCHAR(40)   NOT NULL,  -- e.g. MOTOR_LISTO
    title          VARCHAR(150)  NOT NULL,
    message        VARCHAR(1000) NOT NULL,
    orden_id       BIGINT,
    solicitud_id   BIGINT,
    cliente_email  VARCHAR(200),
    channel        VARCHAR(30)   NOT NULL,  -- WEB | WHATSAPP | EMAIL
    sent           BOOLEAN       NOT NULL DEFAULT false,
    read_at        TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL DEFAULT now(),
    metadata_json  TEXT
    );

CREATE INDEX IF NOT EXISTS idx_notification_email  ON notification (cliente_email);
CREATE INDEX IF NOT EXISTS idx_notification_orden  ON notification (orden_id);
