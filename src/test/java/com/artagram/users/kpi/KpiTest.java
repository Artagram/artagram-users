package com.artagram.users.kpi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.users.common.ApiException;
import com.artagram.users.common.JwtUserResolver;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.service.ActividadEnVivoService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class KpiTest {

    private EventoNegocioRepository eventos;
    private UsuarioRepository usuarios;
    private KpiService service;

    @BeforeEach
    void setUp() {
        eventos = mock(EventoNegocioRepository.class);
        usuarios = mock(UsuarioRepository.class);
        service = new KpiService(eventos, usuarios);
    }

    private static Usuario creadoHace(int dias, boolean premium) throws Exception {
        Usuario u = new Usuario(UUID.randomUUID(), "u", "U", "u@c.com", premium);
        Field f = Usuario.class.getDeclaredField("creadoEn");
        f.setAccessible(true);
        f.set(u, Instant.now().minus(dias, ChronoUnit.DAYS));
        return u;
    }

    @Test
    void sinDatosTodosLosKpisSonCeroEIguales() {
        var kpis = service.calcularTodos();

        assertThat(kpis).hasSize(4).allSatisfy(k -> {
            assertThat(k.valorActual()).isZero();
            assertThat(k.tendencia()).isEqualTo("IGUAL");
        });
        assertThatThrownBy(() -> service.calcular("otro")).isInstanceOf(ApiException.class);
    }

    @Test
    void tasaDeColaboracionYDuracionSuben() {
        when(eventos.contarUsuariosDistintos(eq(TipoEventoNegocio.LOGIN), any(), any())).thenReturn(10L, 0L);
        when(eventos.contarUsuariosDistintos(eq(TipoEventoNegocio.ENTRO_COLABORATIVO), any(), any())).thenReturn(4L, 0L);
        when(eventos.promedioDuracionMs(eq(TipoEventoNegocio.SALIO_COLABORATIVO), any(), any())).thenReturn(600_000.0, 1_200_000.0);
        when(eventos.contarEventos(eq(TipoEventoNegocio.SALIO_COLABORATIVO), any(), any())).thenReturn(3L);

        var tasa = service.calcular("tasa-colaboracion");
        var duracion = service.calcular("duracion-sesion");

        assertThat(tasa.valorActual()).isEqualTo(0.4);
        assertThat(tasa.tendencia()).isEqualTo("SUBE");
        assertThat(duracion.valorActual()).isEqualTo(10.0);
        assertThat(duracion.tendencia()).isEqualTo("BAJA");
        assertThat(duracion.cantidadEventosContados()).isEqualTo(3);
    }

    @Test
    void retencionYConversionPremium() throws Exception {
        Usuario volvio = creadoHace(10, true);
        Usuario noVolvio = creadoHace(9, false);
        Usuario viejo = creadoHace(17, false);
        when(usuarios.findAll()).thenReturn(List.of(volvio, noVolvio, viejo));
        for (Usuario u : List.of(volvio, noVolvio, viejo)) {
            when(usuarios.findById(u.getId())).thenReturn(Optional.of(u));
        }
        when(eventos.existsByUsuarioIdAndTipoAndOcurridoEnBetween(eq(volvio.getId()), eq(TipoEventoNegocio.LOGIN), any(), any()))
                .thenReturn(true);
        UUID fantasma = UUID.randomUUID();
        when(eventos.usuariosDistintos(eq(TipoEventoNegocio.LOGIN), any(), any()))
                .thenReturn(List.of(volvio.getId(), noVolvio.getId(), fantasma), List.of(noVolvio.getId()));

        var retencion = service.calcular("retencion-7-dias");
        var conversion = service.calcular("conversion-premium");

        assertThat(retencion.valorActual()).isEqualTo(0.5);
        assertThat(retencion.cantidadEventosContados()).isEqualTo(2);
        assertThat(retencion.tendencia()).isEqualTo("SUBE");
        assertThat(conversion.valorActual()).isEqualTo(1.0 / 3);
        assertThat(conversion.tendencia()).isEqualTo("SUBE");
    }

    @Test
    void lasMetricasPublicanLosCuatroGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new KpiMetrics(registry, service);

        assertThat(registry.get("kpi_tasa_colaboracion").gauge().value()).isZero();
        assertThat(registry.get("kpi_duracion_sesion_minutos").gauge().value()).isZero();
        assertThat(registry.get("kpi_retencion_7_dias").gauge()).isNotNull();
        assertThat(registry.get("kpi_conversion_premium").gauge()).isNotNull();
    }

    @Test
    void elListenerRegistraLoginsYSesiones() {
        ActividadEnVivoService actividad = mock(ActividadEnVivoService.class);
        KpiEventListener listener = new KpiEventListener(eventos, actividad);
        UUID u = UUID.randomUUID();
        UUID lienzo = UUID.randomUUID();

        listener.onLogin(Map.of("usuarioId", u.toString()));
        listener.onLogin(Map.of("usuarioId", "mal"));
        listener.onSesionIniciada(Map.of("usuarioId", u.toString()));
        listener.onSesionIniciada(Map.of("usuarioId", u.toString(), "actividadId", "a1", "tipo", "LIENZO", "recursoId", lienzo.toString()));
        listener.onSesionIniciada(Map.of("usuarioId", "mal"));
        listener.onSesionTerminada(Map.of("usuarioId", u.toString(), "duracionMs", 5000));
        listener.onSesionTerminada(Map.of("usuarioId", u.toString(), "duracionMs", 7000L, "actividadId", "a1"));
        listener.onSesionTerminada(Map.of("usuarioId", u.toString()));

        ArgumentCaptor<EventoNegocio> guardado = ArgumentCaptor.forClass(EventoNegocio.class);
        verify(eventos, org.mockito.Mockito.times(5)).save(guardado.capture());
        EventoNegocio ultimo = guardado.getAllValues().get(4);
        assertThat(ultimo.getTipo()).isEqualTo(TipoEventoNegocio.SALIO_COLABORATIVO);
        assertThat(ultimo.getDuracionMs()).isEqualTo(7000L);
        assertThat(ultimo.getUsuarioId()).isEqualTo(u);
        assertThat(ultimo.getOcurridoEn()).isNotNull();
        assertThat(ultimo.getId()).isNull();
        verify(actividad).iniciar("a1", u, "LIENZO", lienzo);
        verify(actividad).terminar("a1");
    }

    @Test
    void controladoresSoloParaAdmin() {
        JwtUserResolver resolver = mock(JwtUserResolver.class);
        MockHttpServletRequest req = new MockHttpServletRequest();
        UUID u = UUID.randomUUID();
        when(resolver.resolverRequerido(req))
                .thenReturn(new JwtUserResolver.UsuarioAutenticado(u, "a", null, "ADMIN", false));
        KpiController kpis = new KpiController(service, resolver);
        DashboardController dashboard = new DashboardController(resolver);
        ReflectionTestUtils.setField(dashboard, "panelTecnicoUrl", "http://g/t");
        ReflectionTestUtils.setField(dashboard, "panelNegocioUrl", "http://g/n");
        ReflectionTestUtils.setField(dashboard, "prometheusUrl", "http://p");

        assertThat(kpis.todos(req)).hasSize(4);
        assertThat(kpis.detalle(req, "duracion-sesion").id()).isEqualTo("duracion-sesion");
        assertThat(dashboard.obtener(req).panelTecnicoUrl()).isEqualTo("http://g/t");

        MockHttpServletRequest noAdmin = new MockHttpServletRequest();
        when(resolver.resolverRequerido(noAdmin))
                .thenReturn(new JwtUserResolver.UsuarioAutenticado(u, "a", null, "USUARIO", false));
        assertThatThrownBy(() -> kpis.todos(noAdmin)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> dashboard.obtener(noAdmin)).isInstanceOf(ApiException.class);
        verify(eventos, never()).save(any());
    }

    @Test
    void colasDeKpi() {
        KpiEventsRabbitMqConfig c = new KpiEventsRabbitMqConfig();
        TopicExchange ex = new TopicExchange("artagram.events");
        assertThat(c.usersLoginBinding(c.usersLoginQueue(), ex).getRoutingKey()).isEqualTo("user.login");
        assertThat(c.usersCollabSessionStartedBinding(c.usersCollabSessionStartedQueue(), ex).getRoutingKey())
                .isEqualTo("collab.session.started");
        assertThat(c.usersCollabSessionEndedBinding(c.usersCollabSessionEndedQueue(), ex).getRoutingKey())
                .isEqualTo("collab.session.ended");
        assertThat(TipoEventoNegocio.values()).hasSize(3);
    }
}
