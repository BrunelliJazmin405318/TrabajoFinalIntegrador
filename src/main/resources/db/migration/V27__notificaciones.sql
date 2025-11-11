CREATE TABLE IF NOT EXISTS notificacion (
                                            id           BIGSERIAL PRIMARY KEY,
                                            orden_id     BIGINT NOT NULL,
                                            nro_orden    VARCHAR(30) NOT NULL,
    canal        VARCHAR(20) NOT NULL,        -- "IN_APP" | "WHATSAPP" (mock)
    tipo         VARCHAR(40) NOT NULL,        -- "LISTO_RETIRAR"
    mensaje      TEXT NOT NULL,
    estado       VARCHAR(20) NOT NULL,        -- "PENDIENTE" | "ENVIADA" | "ERROR" | "LEIDA"
    cliente_destino VARCHAR(80),              -- tel o email si lo tenés; opcional
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    read_at      TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_notif_nro_orden ON notificacion(nro_orden);