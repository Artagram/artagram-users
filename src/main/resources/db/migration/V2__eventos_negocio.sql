CREATE TABLE IF NOT EXISTS eventos_negocio (
    id UUID PRIMARY KEY,
    tipo VARCHAR(30) NOT NULL,
    usuario_id UUID NOT NULL,
    duracion_ms BIGINT,
    ocurrido_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_eventos_negocio_tipo_ocurrido ON eventos_negocio(tipo, ocurrido_en);
CREATE INDEX IF NOT EXISTS idx_eventos_negocio_usuario ON eventos_negocio(usuario_id);
