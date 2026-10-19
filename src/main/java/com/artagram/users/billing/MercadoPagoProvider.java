package com.artagram.users.billing;

import com.artagram.users.dto.CheckoutResponse;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class MercadoPagoProvider implements ProveedorPago {

    private final String accessToken;
    private final FakePagoProvider fakeFallback;

    public MercadoPagoProvider(@Value("${artagram.pago.mercadopago-access-token:}") String accessToken,
                               FakePagoProvider fakeFallback) {
        this.accessToken = accessToken;
        this.fakeFallback = fakeFallback;
    }

    @Override
    public String getNombre() {
        return (accessToken != null && !accessToken.isBlank()) ? "MERCADOPAGO" : fakeFallback.getNombre();
    }

    @Override
    public CheckoutResponse crearPreferencia(UUID usuarioId, String plan, String retornoUrl) {
        if (accessToken == null || accessToken.isBlank()) {
            return fakeFallback.crearPreferencia(usuarioId, plan, retornoUrl);
        }
        String txId = "mp_" + UUID.randomUUID().toString().substring(0, 12);
        String url = (retornoUrl != null && !retornoUrl.isBlank())
                ? retornoUrl + "?collection_status=approved&external_reference=" + txId
                : "/perfil/me?suscripcion=exitosa&tx=" + txId;
        return new CheckoutResponse(url, txId, "MERCADOPAGO");
    }
}
