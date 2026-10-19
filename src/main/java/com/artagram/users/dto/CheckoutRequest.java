package com.artagram.users.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
        @NotBlank(message = "El plan es obligatorio")
        String plan,
        String retornoUrl
) {
}
