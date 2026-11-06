CREATE TABLE IF NOT EXISTS actividad_en_vivo (
    id VARCHAR(120) PRIMARY KEY,
    usuario_id UUID NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    recurso_id UUID NOT NULL,
    iniciada_en TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_actividad_usuario ON actividad_en_vivo(usuario_id);
