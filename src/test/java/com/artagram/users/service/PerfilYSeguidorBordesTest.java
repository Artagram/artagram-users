package com.artagram.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.users.common.ApiException;
import com.artagram.users.domain.ObraDestacadaRepository;
import com.artagram.users.domain.Perfil;
import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.Seguidor;
import com.artagram.users.domain.SeguidorRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.dto.ActualizarPerfilRequest;
import com.artagram.users.dto.PerfilResponse;
import com.artagram.users.events.UsersEventPublisher;
import com.artagram.users.storage.StorageService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerfilYSeguidorBordesTest {

    private final UUID id = UUID.randomUUID();
    private final Usuario usuario = new Usuario(id, "ana", "Ana", "a@c.com", false);
    private PerfilRepository perfiles;
    private ObraDestacadaRepository obras;
    private UsuarioRepository usuarios;
    private PerfilService perfilService;

    @BeforeEach
    void setUp() {
        perfiles = mock(PerfilRepository.class);
        obras = mock(ObraDestacadaRepository.class);
        usuarios = mock(UsuarioRepository.class);
        perfilService = new PerfilService(perfiles, obras, usuarios, mock(StorageService.class));
        when(perfiles.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarios.findById(id)).thenReturn(Optional.of(usuario));
    }

    @Test
    void perfilPublicoPorNombreArtistaOUsuario() {
        when(usuarios.findByNombreArtistaIgnoreCase("Ana")).thenReturn(Optional.of(usuario));
        when(usuarios.findByNombreArtistaIgnoreCase("ana")).thenReturn(Optional.empty());
        when(usuarios.findByNombreUsuarioIgnoreCase("ana")).thenReturn(Optional.of(usuario));
        when(usuarios.findByNombreArtistaIgnoreCase("nadie")).thenReturn(Optional.empty());
        when(usuarios.findByNombreUsuarioIgnoreCase("nadie")).thenReturn(Optional.empty());
        when(perfiles.findByUsuarioId(id)).thenReturn(Optional.empty());

        assertThat(perfilService.obtenerPerfilPublico("Ana", id).nombreArtista()).isEqualTo("Ana");
        assertThat(perfilService.obtenerPerfilPublico("ana", null)).isNotNull();
        assertThat(perfilService.obtenerPerfilPublico("ana", UUID.randomUUID())).isNotNull();
        assertThatThrownBy(() -> perfilService.obtenerPerfilPublico("nadie", null)).isInstanceOf(ApiException.class);
    }

    @Test
    void actualizarPerfilCompleto() {
        when(perfiles.findByUsuarioId(id)).thenReturn(Optional.empty());

        var dto = perfilService.actualizarPerfil(id, new ActualizarPerfilRequest(" Ana Art ", "bio", "/f.png",
                List.of(new PerfilResponse.EnlaceDto("web", "https://a"), new PerfilResponse.EnlaceDto(null, "x")),
                List.of(new PerfilResponse.ObraDestacadaDto("Mar", "/m.png"), new PerfilResponse.ObraDestacadaDto("Río", "/r.png"))));

        assertThat(usuario.getNombreArtista()).isEqualTo("Ana Art");
        assertThat(dto.biografia()).isEqualTo("bio");
        verify(obras).deleteByPerfilId(any());
        verify(obras, times(2)).save(any());
    }

    @Test
    void actualizarPerfilSinCambiosOpcionales() {
        Perfil existente = new Perfil(UUID.randomUUID(), usuario, "vieja", null);
        when(perfiles.findByUsuarioId(id)).thenReturn(Optional.of(existente));

        perfilService.actualizarPerfil(id, new ActualizarPerfilRequest(" ", null, null, null, null));

        assertThat(existente.getBiografia()).isEqualTo("vieja");
        verify(usuarios, never()).save(any());
        verify(obras, never()).deleteByPerfilId(any());
        UUID nadie = UUID.randomUUID();
        when(usuarios.findById(nadie)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> perfilService.actualizarPerfil(nadie, new ActualizarPerfilRequest(null, null, null, null, null)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void seguirYDejarDeSeguirPublicaLosEventos() {
        SeguidorRepository seguidores = mock(SeguidorRepository.class);
        UsersEventPublisher eventos = mock(UsersEventPublisher.class);
        SeguidorService service = new SeguidorService(seguidores, usuarios, eventos);
        UUID otro = UUID.randomUUID();
        when(seguidores.existsById_SeguidorIdAndId_SeguidoId(id, otro)).thenReturn(false, true);
        when(seguidores.countById_SeguidoId(otro)).thenReturn(1L, 0L);
        UUID sinUsuario = UUID.randomUUID();
        when(usuarios.findById(sinUsuario)).thenReturn(Optional.empty());

        assertThat(service.toggleSeguir(id, otro).loSigo()).isTrue();
        assertThat(service.toggleSeguir(id, otro).loSigo()).isFalse();
        service.toggleSeguir(sinUsuario, otro);
        assertThatThrownBy(() -> service.toggleSeguir(id, id)).isInstanceOf(ApiException.class);

        verify(seguidores, times(2)).save(any(Seguidor.class));
        verify(eventos).publicarFollowCreado(id, "Ana", otro);
        verify(eventos).publicarFollowCreado(sinUsuario, "Un artista", otro);
        verify(eventos).publicarFollowEliminado(id, otro);
    }
}
