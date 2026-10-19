package com.artagram.users.common;

import java.time.Instant;

public record ErrorResponse(
        String codigo,
        String mensaje,
        Instant timestamp,
        String traceId
) {
    public ErrorResponse(String codigo, String mensaje, String traceId) {
        this(codigo, mensaje, Instant.now(), traceId);
    }
}
