package com.artagram.users.dto;

import java.util.UUID;

/** enComun cuenta a quienes sigues que tambien siguen a este artista; 0 si salio por popularidad. */
public record ArtistaRecomendado(
        UUID usuarioId,
        String nombreArtista,
        String fotoUrl,
        long seguidores,
        long enComun
) {
}
