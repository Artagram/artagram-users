package com.artagram.users.service;

import com.artagram.users.billing.ProveedorPago;
import com.artagram.users.common.ApiException;
import com.artagram.users.domain.Suscripcion;
import com.artagram.users.domain.SuscripcionRepository;
import com.artagram.users.domain.UsoCuenta;
import com.artagram.users.domain.UsoCuentaRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.dto.CheckoutRequest;
import com.artagram.users.dto.CheckoutResponse;
import com.artagram.users.dto.PlanDetalleDto;
import com.artagram.users.dto.SuscripcionDto;
import com.artagram.users.events.UsersEventPublisher;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepo;
    private final UsoCuentaRepository usoRepo;
    private final UsuarioRepository usuarioRepo;
    private final ProveedorPago proveedorPago;
    private final UsersEventPublisher eventPublisher;

    public SuscripcionService(SuscripcionRepository suscripcionRepo,
                              UsoCuentaRepository usoRepo,
                              UsuarioRepository usuarioRepo,
                              ProveedorPago proveedorPago,
                              UsersEventPublisher eventPublisher) {
        this.suscripcionRepo = suscripcionRepo;
        this.usoRepo = usoRepo;
        this.usuarioRepo = usuarioRepo;
        this.proveedorPago = proveedorPago;
        this.eventPublisher = eventPublisher;
    }

    public SuscripcionDto obtenerSuscripcion(UUID usuarioId) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));

        UsoCuenta uso = usoRepo.findByUsuarioId(usuarioId)
                .orElseGet(() -> usoRepo.save(new UsoCuenta(UUID.randomUUID(), usuarioId, 0, usuario.isPremium() ? 5120 : 512, 0, usuario.isPremium() ? 500 : 50)));

        Suscripcion sub = suscripcionRepo.findTopByUsuarioIdOrderByFechaInicioDesc(usuarioId)
                .orElseGet(() -> new Suscripcion(UUID.randomUUID(), usuarioId, "GRATIS", "ACTIVA", Instant.now(), null, "NINGUNO", null));

        return new SuscripcionDto(
                sub.getPlan(),
                sub.getEstado(),
                sub.getFechaInicio().toString(),
                sub.getFechaFin() != null ? sub.getFechaFin().toString() : null,
                usuario.isPremium(),
                uso.getMegabytesUsados(),
                uso.getMaxMegabytes(),
                uso.getObrasSubidas(),
                uso.getMaxObras()
        );
    }

    public CheckoutResponse crearCheckout(UUID usuarioId, CheckoutRequest req) {
        return proveedorPago.crearPreferencia(usuarioId, req.plan(), req.retornoUrl());
    }

    @Transactional
    public void procesarPagoAprobado(UUID usuarioId, String idTransaccion) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));

        usuario.setPremium(true);
        usuarioRepo.save(usuario);

        Instant ahora = Instant.now();
        Instant fin = ahora.plus(30, ChronoUnit.DAYS);

        Suscripcion sub = new Suscripcion(
                UUID.randomUUID(), usuarioId, "PRO", "ACTIVA", ahora, fin, proveedorPago.getNombre(), idTransaccion
        );
        suscripcionRepo.save(sub);

        UsoCuenta uso = usoRepo.findByUsuarioId(usuarioId).orElseGet(() -> new UsoCuenta(UUID.randomUUID(), usuarioId, 0, 5120, 0, 500));
        uso.setMaxMegabytes(5120);
        uso.setMaxObras(500);
        usoRepo.save(uso);

        eventPublisher.publicarSuscripcionActualizada(usuarioId, true, "PRO");
    }

    public PlanDetalleDto obtenerPlanDetalle(UUID usuarioId) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
        UsoCuenta uso = usoRepo.findByUsuarioId(usuarioId)
                .orElseGet(() -> new UsoCuenta(UUID.randomUUID(), usuarioId, 0, usuario.isPremium() ? 5120 : 512, 0, usuario.isPremium() ? 500 : 50));

        return new PlanDetalleDto(usuarioId, usuario.isPremium() ? "PRO" : "GRATIS", usuario.isPremium(), uso.getMaxMegabytes(), uso.getMaxObras());
    }
}
