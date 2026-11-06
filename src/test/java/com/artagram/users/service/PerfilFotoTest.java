package com.artagram.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.artagram.users.common.ApiException;
import com.artagram.users.domain.ObraDestacadaRepository;
import com.artagram.users.domain.Perfil;
import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.storage.StorageService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class PerfilFotoTest {

    private final UUID usuarioId = UUID.randomUUID();
    private final PerfilRepository perfiles = mock(PerfilRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final StorageService storage = mock(StorageService.class);
    private final PerfilService service = new PerfilService(perfiles, mock(ObraDestacadaRepository.class), usuarios, storage);

    private MockMultipartFile foto(String tipo) {
        return new MockMultipartFile("archivo", "yo", tipo, new byte[]{1, 2});
    }

    @Test
    void guardaLaFotoAunqueElPerfilNoExistieraTodavia() {
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(mock(Usuario.class)));
        when(perfiles.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        when(storage.guardar(eq("avatars"), eq("foto.png"), any(), eq("image/png"))).thenReturn("avatars/a.png");
        when(storage.resolverUrlPublica("avatars/a.png")).thenReturn("/media/avatars/a.png");

        assertThat(service.actualizarFoto(usuarioId, foto("image/png"))).isEqualTo("/media/avatars/a.png");

        ArgumentCaptor<Perfil> perfil = ArgumentCaptor.forClass(Perfil.class);
        verify(perfiles).save(perfil.capture());
        assertThat(perfil.getValue().getFotoUrl()).isEqualTo("avatars/a.png");
    }

    @Test
    void rechazaFormatosInseguros() {
        assertThatThrownBy(() -> service.actualizarFoto(usuarioId, foto("image/svg+xml")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("FORMATO_NO_SOPORTADO"));
        verifyNoInteractions(storage);
    }

    @Test
    void exigeUnArchivo() {
        assertThatThrownBy(() -> service.actualizarFoto(usuarioId, null)).isInstanceOf(ApiException.class);
    }

    @Test
    void unUsuarioInexistenteNoSubeNada() {
        when(usuarios.findById(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarFoto(usuarioId, foto("image/png"))).isInstanceOf(ApiException.class);
        verifyNoInteractions(storage);
    }
}
