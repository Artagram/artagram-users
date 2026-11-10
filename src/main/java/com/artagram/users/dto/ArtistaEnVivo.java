package com.artagram.users.dto;

import java.util.UUID;

/** comunidadId queda en null en Modo Fiesta: las salas son privadas por codigo. */
public record ArtistaEnVivo(
        UUID usuarioId,
        String nombreArtista,
        String fotoUrl,
        String tipo,
        UUID comunidadId,
        String iniciadaEn
) {
}
