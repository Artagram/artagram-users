package com.artagram.users.dto;

import java.util.UUID;

public record ArtistaResumen(
        UUID usuarioId,
        String nombreArtista,
        boolean loSigo,
        long seguidores
) {
}
