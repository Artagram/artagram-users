package com.artagram.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.artagram.users.domain.ActividadEnVivo;
import com.artagram.users.domain.ActividadEnVivoRepository;
import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.storage.StorageService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActividadEnVivoServiceTest {

    private final UUID yo = UUID.randomUUID();
    private final UUID artistaId = UUID.randomUUID();
    private final UUID comunidadId = UUID.randomUUID();
    private final ActividadEnVivoRepository actividades = mock(ActividadEnVivoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PerfilRepository perfiles = mock(PerfilRepository.class);
    private final ActividadEnVivoService service =
            new ActividadEnVivoService(actividades, usuarios, perfiles, mock(StorageService.class));

    @BeforeEach
    void setUp() {
        Usuario artista = mock(Usuario.class);
        when(artista.getId()).thenReturn(artistaId);
        when(artista.getNombreArtista()).thenReturn("lucia");
        when(usuarios.findById(artistaId)).thenReturn(Optional.of(artista));
        when(perfiles.findByUsuarioId(artistaId)).thenReturn(Optional.empty());
    }

    private void actividad(ActividadEnVivo... filas) {
        when(actividades.buscarDeSeguidos(eq(yo), any())).thenReturn(List.of(filas));
    }

    @Test
    void elLienzoIncluyeLaComunidad() {
        actividad(new ActividadEnVivo("a", artistaId, "LIENZO", comunidadId));

        assertThat(service.siguiendo(yo).get(0).comunidadId()).isEqualTo(comunidadId);
    }

    @Test
    void laSalaDeFiestaNoSeRevela() {
        actividad(new ActividadEnVivo("a", artistaId, "FIESTA", UUID.randomUUID()));

        var artista = service.siguiendo(yo).get(0);
        assertThat(artista.tipo()).isEqualTo("FIESTA");
        assertThat(artista.comunidadId()).isNull();
    }

    @Test
    void unArtistaEnVariasPestanasSaleUnaVezYSePrefiereElLienzo() {
        actividad(new ActividadEnVivo("a", artistaId, "FIESTA", UUID.randomUUID()),
                new ActividadEnVivo("b", artistaId, "LIENZO", comunidadId));

        var lista = service.siguiendo(yo);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).tipo()).isEqualTo("LIENZO");
    }

    @Test
    void ignoraActividadDeCuentasEliminadas() {
        actividad(new ActividadEnVivo("a", UUID.randomUUID(), "LIENZO", comunidadId));

        assertThat(service.siguiendo(yo)).isEmpty();
    }
}
