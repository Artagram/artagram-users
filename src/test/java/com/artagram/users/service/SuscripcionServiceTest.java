package com.artagram.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.users.billing.FakePagoProvider;
import com.artagram.users.billing.MercadoPagoProvider;
import com.artagram.users.common.ApiException;
import com.artagram.users.domain.Suscripcion;
import com.artagram.users.domain.SuscripcionRepository;
import com.artagram.users.domain.UsoCuenta;
import com.artagram.users.domain.UsoCuentaRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.dto.CheckoutRequest;
import com.artagram.users.events.UsersEventPublisher;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SuscripcionServiceTest {

    private final UUID id = UUID.randomUUID();
    private SuscripcionRepository subs;
    private UsoCuentaRepository usos;
    private UsuarioRepository usuarios;
    private UsersEventPublisher eventos;
    private SuscripcionService service;

    @BeforeEach
    void setUp() {
        subs = mock(SuscripcionRepository.class);
        usos = mock(UsoCuentaRepository.class);
        usuarios = mock(UsuarioRepository.class);
        eventos = mock(UsersEventPublisher.class);
        when(usos.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new SuscripcionService(subs, usos, usuarios, new MercadoPagoProvider("", new FakePagoProvider()), eventos);
    }

    @Test
    void usuarioNuevoGratisRecibeValoresPorDefecto() {
        when(usuarios.findById(id)).thenReturn(Optional.of(new Usuario(id, "a", "A", "a@c.com", false)));

        var dto = service.obtenerSuscripcion(id);
        var plan = service.obtenerPlanDetalle(id);

        assertThat(dto.plan()).isEqualTo("GRATIS");
        assertThat(dto.fechaFin()).isNull();
        assertThat(dto.maxMegabytes()).isEqualTo(512);
        assertThat(plan.plan()).isEqualTo("GRATIS");
        assertThat(plan.maxObras()).isEqualTo(50);
    }

    @Test
    void usuarioProConDatosGuardados() {
        Instant fin = Instant.now().plus(5, ChronoUnit.DAYS);
        when(usuarios.findById(id)).thenReturn(Optional.of(new Usuario(id, "a", "A", "a@c.com", true)));
        when(usos.findByUsuarioId(id)).thenReturn(Optional.of(new UsoCuenta(UUID.randomUUID(), id, 30, 5120, 2, 500)));
        when(subs.findTopByUsuarioIdOrderByFechaInicioDesc(id))
                .thenReturn(Optional.of(new Suscripcion(UUID.randomUUID(), id, "PRO", "ACTIVA", Instant.now(), fin, "FAKE", "tx")));

        var dto = service.obtenerSuscripcion(id);

        assertThat(dto.premium()).isTrue();
        assertThat(dto.fechaFin()).isEqualTo(fin.toString());
        assertThat(dto.megabytesUsados()).isEqualTo(30);
        assertThat(service.obtenerPlanDetalle(id).plan()).isEqualTo("PRO");
    }

    @Test
    void pagoAprobadoActivaElPlanPro() {
        Usuario u = new Usuario(id, "a", "A", "a@c.com", false);
        when(usuarios.findById(id)).thenReturn(Optional.of(u));

        service.procesarPagoAprobado(id, "tx-1");

        assertThat(u.isPremium()).isTrue();
        verify(subs).save(any(Suscripcion.class));
        verify(eventos).publicarSuscripcionActualizada(id, true, "PRO");
    }

    @Test
    void usuarioInexistenteEs404() {
        when(usuarios.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtenerSuscripcion(id)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.obtenerPlanDetalle(id)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.procesarPagoAprobado(id, "tx")).isInstanceOf(ApiException.class);
    }

    @Test
    void checkoutConProveedorRealOSimulado() {
        var fake = service.crearCheckout(id, new CheckoutRequest("PRO", null));
        assertThat(fake.proveedor()).isEqualTo("FAKE");
        assertThat(fake.checkoutUrl()).startsWith("/perfil/me?suscripcion=exitosa");

        MercadoPagoProvider mp = new MercadoPagoProvider("token", new FakePagoProvider());
        assertThat(mp.getNombre()).isEqualTo("MERCADOPAGO");
        assertThat(mp.crearPreferencia(id, "PRO", "http://r").checkoutUrl()).startsWith("http://r?collection_status=approved");
        assertThat(mp.crearPreferencia(id, "PRO", " ").checkoutUrl()).startsWith("/perfil/me");
        assertThat(new MercadoPagoProvider(null, new FakePagoProvider()).getNombre()).isEqualTo("FAKE");
        assertThat(new FakePagoProvider().crearPreferencia(id, "PRO", "http://r").checkoutUrl()).startsWith("http://r?");
    }
}
