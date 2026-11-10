package com.artagram.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.SeguidorRepository;
import com.artagram.users.domain.SeguidorRepository.ArtistaEnComun;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.storage.StorageService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class RecomendacionArtistasServiceTest {

    private final UUID yo = UUID.randomUUID();
    private final SeguidorRepository seguidores = mock(SeguidorRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RecomendacionArtistasService service =
            new RecomendacionArtistasService(seguidores, usuarios, mock(PerfilRepository.class), mock(StorageService.class));

    private UUID artista(String nombre) {
        UUID id = UUID.randomUUID();
        Usuario u = mock(Usuario.class);
        when(u.getId()).thenReturn(id);
        when(u.getNombreArtista()).thenReturn(nombre);
        when(usuarios.findById(id)).thenReturn(Optional.of(u));
        return id;
    }

    private ArtistaEnComun enComun(UUID id, long cantidad) {
        return new ArtistaEnComun() {
            @Override public UUID getArtistaId() { return id; }
            @Override public long getEnComun() { return cantidad; }
        };
    }

    @Test
    void completaConPopularesSinRepetir() {
        UUID cercano = artista("cercano");
        UUID popular = artista("popular");
        when(seguidores.buscarSeguidosPorMisSeguidos(eq(yo), any())).thenReturn(List.of(enComun(cercano, 3)));
        when(seguidores.buscarMasSeguidos(eq(yo), any())).thenReturn(List.of(cercano, popular));

        var lista = service.recomendar(yo, 5);

        assertThat(lista).extracting("nombreArtista").containsExactly("cercano", "popular");
        assertThat(lista).extracting("enComun").containsExactly(3L, 0L);
    }

    @Test
    void noBuscaPopularesSiYaAlcanza() {
        UUID cercano = artista("cercano");
        when(seguidores.buscarSeguidosPorMisSeguidos(eq(yo), any())).thenReturn(List.of(enComun(cercano, 1)));

        assertThat(service.recomendar(yo, 1)).hasSize(1);
        verify(seguidores, org.mockito.Mockito.never()).buscarMasSeguidos(any(), any());
    }

    @Test
    void elLimiteNoPasaDeVeinte() {
        service.recomendar(yo, 500);

        verify(seguidores).buscarSeguidosPorMisSeguidos(yo, PageRequest.of(0, 20));
    }
}
