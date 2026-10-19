CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    nombre_artista VARCHAR(100) NOT NULL,
    correo VARCHAR(100) NOT NULL UNIQUE,
    premium BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS perfiles (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    biografia TEXT,
    foto_url VARCHAR(500),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP
);

CREATE TABLE IF NOT EXISTS enlaces_perfil (
    perfil_id UUID NOT NULL REFERENCES perfiles(id) ON DELETE CASCADE,
    etiqueta VARCHAR(50) NOT NULL,
    url VARCHAR(500) NOT NULL,
    PRIMARY KEY (perfil_id, etiqueta, url)
);

CREATE TABLE IF NOT EXISTS obras_destacadas (
    id UUID PRIMARY KEY,
    perfil_id UUID NOT NULL REFERENCES perfiles(id) ON DELETE CASCADE,
    titulo VARCHAR(150) NOT NULL,
    imagen_url VARCHAR(500) NOT NULL,
    orden INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS seguidores (
    seguidor_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    seguido_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (seguidor_id, seguido_id)
);

CREATE TABLE IF NOT EXISTS suscripciones (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    plan VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMP,
    proveedor_pago VARCHAR(30),
    id_transaccion VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS uso_cuenta (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    megabytes_usados BIGINT NOT NULL DEFAULT 0,
    max_megabytes BIGINT NOT NULL DEFAULT 512,
    obras_subidas INT NOT NULL DEFAULT 0,
    max_obras INT NOT NULL DEFAULT 50
);

CREATE INDEX IF NOT EXISTS idx_usuarios_nombre_artista ON usuarios(nombre_artista);
CREATE INDEX IF NOT EXISTS idx_seguidores_seguidor ON seguidores(seguidor_id);
CREATE INDEX IF NOT EXISTS idx_seguidores_seguido ON seguidores(seguido_id);
CREATE INDEX IF NOT EXISTS idx_suscripciones_usuario ON suscripciones(usuario_id, estado);
