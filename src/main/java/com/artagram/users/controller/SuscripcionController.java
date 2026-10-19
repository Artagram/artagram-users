package com.artagram.users.controller;

import com.artagram.users.common.JwtUserResolver;
import com.artagram.users.dto.CheckoutRequest;
import com.artagram.users.dto.CheckoutResponse;
import com.artagram.users.dto.SuscripcionDto;
import com.artagram.users.service.SuscripcionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suscripcion")
public class SuscripcionController {

    private final SuscripcionService suscripcionService;
    private final JwtUserResolver userResolver;

    public SuscripcionController(SuscripcionService suscripcionService, JwtUserResolver userResolver) {
        this.suscripcionService = suscripcionService;
        this.userResolver = userResolver;
    }

    @GetMapping("/actual")
    public ResponseEntity<SuscripcionDto> obtenerActual(HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(suscripcionService.obtenerSuscripcion(usuario.id()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest req, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(suscripcionService.crearCheckout(usuario.id(), req));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<Map<String, String>> confirmar(
            @RequestParam(required = false, defaultValue = "sim_manual") String tx,
            HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        suscripcionService.procesarPagoAprobado(usuario.id(), tx);
        return ResponseEntity.ok(Map.of("mensaje", "Suscripción PRO activada exitosamente"));
    }
}
