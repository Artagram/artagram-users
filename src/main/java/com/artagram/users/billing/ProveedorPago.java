package com.artagram.users.billing;

import com.artagram.users.dto.CheckoutResponse;
import java.util.UUID;

public interface ProveedorPago {
    String getNombre();
    CheckoutResponse crearPreferencia(UUID usuarioId, String plan, String retornoUrl);
}
