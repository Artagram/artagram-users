package com.artagram.users.billing;

import com.artagram.users.dto.CheckoutResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FakePagoProvider implements ProveedorPago {

    @Override
    public String getNombre() {
        return "FAKE";
    }

    @Override
    public CheckoutResponse crearPreferencia(UUID usuarioId, String plan, String retornoUrl) {
        String txId = "sim_" + UUID.randomUUID().toString().substring(0, 12);
        String url = (retornoUrl != null && !retornoUrl.isBlank())
                ? retornoUrl + "?collection_status=approved&external_reference=" + txId
                : "/perfil/me?suscripcion=exitosa&tx=" + txId;
        return new CheckoutResponse(url, txId, "FAKE");
    }
}
