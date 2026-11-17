package com.artagram.users.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DominioUsersTest {

    private final UUID id = UUID.randomUUID();

    @Test
    void suscripcionYUsoDeCuenta() {
        Suscripcion s = new Suscripcion(id, id, "GRATIS", "ACTIVA", Instant.now(), null, "NINGUNO", null);
        s.setPlan("PRO");
        s.setEstado("CANCELADA");
        Instant fin = Instant.now();
        s.setFechaFin(fin);
        s.setProveedorPago("FAKE");
        s.setIdTransaccion("tx");
        assertThat(s.getId()).isEqualTo(id);
        assertThat(s.getUsuarioId()).isEqualTo(id);
        assertThat(s.getPlan()).isEqualTo("PRO");
        assertThat(s.getEstado()).isEqualTo("CANCELADA");
        assertThat(s.getFechaInicio()).isNotNull();
        assertThat(s.getFechaFin()).isEqualTo(fin);
        assertThat(s.getProveedorPago()).isEqualTo("FAKE");
        assertThat(s.getIdTransaccion()).isEqualTo("tx");
        assertThat(new Suscripcion().getId()).isNull();

        UsoCuenta u = new UsoCuenta(id, id, 1, 2, 3, 4);
        u.setMegabytesUsados(5);
        u.setMaxMegabytes(6);
        u.setObrasSubidas(7);
        u.setMaxObras(8);
        assertThat(u.getId()).isEqualTo(id);
        assertThat(u.getUsuarioId()).isEqualTo(id);
        assertThat(u.getMegabytesUsados()).isEqualTo(5);
        assertThat(u.getMaxMegabytes()).isEqualTo(6);
        assertThat(u.getObrasSubidas()).isEqualTo(7);
        assertThat(u.getMaxObras()).isEqualTo(8);
    }

    @Test
    void perfilObrasEnlacesYSeguidores() {
        Usuario usuario = new Usuario(id, "ana", "Ana", "a@c.com", false);
        usuario.setNombreUsuario("ana2");
        usuario.setNombreArtista("Ana 2");
        usuario.setCorreo("b@c.com");
        Perfil perfil = new Perfil(id, usuario, "bio", "/f.png");
        perfil.setUsuario(usuario);
        perfil.setBiografia("nueva");
        perfil.setFotoUrl("/g.png");
        perfil.setActualizadoEn(Instant.now());
        perfil.setEnlaces(Set.of(new EnlacePerfil("web", "https://ana.art")));
        assertThat(perfil.getUsuario().getNombreArtista()).isEqualTo("Ana 2");
        assertThat(perfil.getBiografia()).isEqualTo("nueva");
        assertThat(perfil.getEnlaces()).extracting(EnlacePerfil::getUrl).containsExactly("https://ana.art");
        assertThat(perfil.getCreadoEn()).isNotNull();
        assertThat(perfil.getActualizadoEn()).isNotNull();

        ObraDestacada obra = new ObraDestacada(id, perfil, "Mar", "/m.png", 1);
        obra.setTitulo("Río");
        obra.setImagenUrl("/r.png");
        obra.setOrden(2);
        assertThat(obra.getId()).isEqualTo(id);
        assertThat(obra.getPerfil()).isSameAs(perfil);
        assertThat(obra.getTitulo()).isEqualTo("Río");
        assertThat(obra.getImagenUrl()).isEqualTo("/r.png");
        assertThat(obra.getOrden()).isEqualTo(2);
        assertThat(new ObraDestacada().getId()).isNull();

        EnlacePerfil enlace = new EnlacePerfil("web", "https://a");
        Object otroTipo = "x";
        assertThat(enlace.equals(enlace)).isTrue();
        assertThat(enlace.equals(otroTipo)).isFalse();
        assertThat(enlace).isEqualTo(new EnlacePerfil("web", "https://a"))
                .isNotEqualTo(new EnlacePerfil("web", "https://b"));
        assertThat(enlace.hashCode()).isEqualTo(new EnlacePerfil("web", "https://a").hashCode());
        assertThat(new EnlacePerfil().getEtiqueta()).isNull();

        UUID otro = UUID.randomUUID();
        Seguidor.SeguidorId sid = new Seguidor(id, otro).getId();
        assertThat(sid.equals(sid)).isTrue();
        assertThat(sid.equals(otroTipo)).isFalse();
        assertThat(sid).isEqualTo(new Seguidor.SeguidorId(id, otro))
                .isNotEqualTo(new Seguidor.SeguidorId(otro, id));
        assertThat(sid.hashCode()).isEqualTo(new Seguidor.SeguidorId(id, otro).hashCode());
        assertThat(sid.getSeguidorId()).isEqualTo(id);
        assertThat(sid.getSeguidoId()).isEqualTo(otro);
        assertThat(new Seguidor(id, otro).getCreadoEn()).isNotNull();
        assertThat(new Seguidor().getId()).isNull();
        assertThat(new Seguidor.SeguidorId().getSeguidorId()).isNull();
    }
}
