package com.artagram.users.dto;

import java.util.List;
import java.util.UUID;

public record PerfilResponse(
        UUID usuarioId,
        String nombreArtista,
        String biografia,
        String fotoUrl,
        List<EnlaceDto> enlaces,
        List<ObraDestacadaDto> obrasDestacadas,
        boolean esPropio,
        boolean premium
) {
    public record EnlaceDto(String etiqueta, String url) {}
    public record ObraDestacadaDto(String titulo, String imagenUrl) {}
}
