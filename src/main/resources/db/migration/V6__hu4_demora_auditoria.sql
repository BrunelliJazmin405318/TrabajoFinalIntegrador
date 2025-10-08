-- V6__hu4_demora_auditoria.sql
-- HU4: Auditoría de cambios + índices. NO tocar demora_motivo (ya existe).

-- Auditoría de cambios (si no existe)
CREATE TABLE IF NOT EXISTS auditoria_cambio (
                                                id BIGSERIAL PRIMARY KEY,
                                                orden_id BIGINT NOT NULL,
                                                campo TEXT NOT NULL,
                                                valor_anterior TEXT,
                                                valor_nuevo TEXT,
                                                usuario TEXT,
                                                fecha TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Índices útiles (idempotentes)
CREATE INDEX IF NOT EXISTS idx_historial_orden ON orden_etapa_historial(orden_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_orden ON auditoria_cambio(orden_id);