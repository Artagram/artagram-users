package com.artagram.users.dto;

import java.util.List;

public record ActualizarPerfilRequest(
        String nombreArtista,
        String biografia,
        String fotoUrl,
        List<PerfilResponse.EnlaceDto> enlaces,
        List<PerfilResponse.ObraDestacadaDto> obrasDestacadas
) {
}
