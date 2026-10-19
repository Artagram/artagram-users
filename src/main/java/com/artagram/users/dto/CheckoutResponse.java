package com.artagram.users.dto;

public record CheckoutResponse(
        String checkoutUrl,
        String idTransaccion,
        String proveedor
) {
}
