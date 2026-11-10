package com.artagram.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.users.domain.SeguidorRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.events.UsersEventPublisher;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SeguidorServiceTest {

    private final UUID yo = UUID.randomUUID();
    private final SeguidorRepository seguidores = mock(SeguidorRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final UsersEventPublisher eventos = mock(UsersEventPublisher.class);
    private final SeguidorService service = new SeguidorService(seguidores, usuarios, eventos);

    @Test
    void laBusquedaIndicaSiYaLoSigoYSusSeguidores() {
        UUID artistaId = UUID.randomUUID();
        Usuario artista = mock(Usuario.class);
        when(artista.getId()).thenReturn(artistaId);
        when(artista.getNombreArtista()).thenReturn("lucia");
        when(usuarios.findTop20ByNombreArtistaContainingIgnoreCaseAndIdNotOrderByNombreArtistaAsc("luc", yo)).thenReturn(List.of(artista));
        when(seguidores.existsById_SeguidorIdAndId_SeguidoId(yo, artistaId)).thenReturn(true);
        when(seguidores.countById_SeguidoId(artistaId)).thenReturn(7L);

        var resultado = service.buscar("  luc ", yo);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).loSigo()).isTrue();
        assertThat(resultado.get(0).seguidores()).isEqualTo(7);
    }

    @Test
    void sinTextoSugiereArtistasExcluyendomeAMi() {
        service.buscar("", yo);

        verify(usuarios).findTop20ByNombreArtistaContainingIgnoreCaseAndIdNotOrderByNombreArtistaAsc("", yo);
    }

    @Test
    void elEventoDeSeguirLlevaElNombreDelSeguidor() {
        UUID seguidoId = UUID.randomUUID();
        Usuario seguidor = mock(Usuario.class);
        when(seguidor.getNombreArtista()).thenReturn("lucia");
        when(usuarios.findById(yo)).thenReturn(Optional.of(seguidor));

        service.toggleSeguir(yo, seguidoId);

        verify(eventos).publicarFollowCreado(yo, "lucia", seguidoId);
    }
}
