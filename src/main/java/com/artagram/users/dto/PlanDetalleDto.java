package com.artagram.users.dto;

import java.util.UUID;

public record PlanDetalleDto(
        UUID usuarioId,
        String plan,
        boolean premium,
        long maxMegabytes,
        int maxObras
) {
}
