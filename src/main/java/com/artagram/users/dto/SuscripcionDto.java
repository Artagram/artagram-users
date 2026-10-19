package com.artagram.users.dto;

public record SuscripcionDto(
        String plan,
        String estado,
        String fechaInicio,
        String fechaFin,
        boolean premium,
        long megabytesUsados,
        long maxMegabytes,
        int obrasSubidas,
        int maxObras
) {
}
