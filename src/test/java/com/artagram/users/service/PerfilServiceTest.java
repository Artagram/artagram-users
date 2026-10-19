package com.artagram.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.artagram.users.common.ApiException;
import com.artagram.users.domain.ObraDestacadaRepository;
import com.artagram.users.domain.Perfil;
import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.dto.PerfilResponse;
import com.artagram.users.storage.StorageService;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    @Mock
    private PerfilRepository perfilRepo;
    @Mock
    private ObraDestacadaRepository obraRepo;
    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private StorageService storageService;

    private PerfilService service;

    @BeforeEach
    void setUp() {
        service = new PerfilService(perfilRepo, obraRepo, usuarioRepo, storageService);
    }

    @Test
    void obtenerPerfilPropioExistente() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario(usuarioId, "lucia_acuarela", "Lucía Mora", "lucia@artagram.local", false);
        Perfil perfil = new Perfil(UUID.randomUUID(), usuario, "Mi bio", "avatar.jpg");

        when(usuarioRepo.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilRepo.findByUsuarioId(usuarioId)).thenReturn(Optional.of(perfil));
        when(obraRepo.findByPerfilIdOrderByOrdenAsc(any())).thenReturn(Collections.emptyList());
        when(storageService.resolverUrlPublica(any())).thenAnswer(inv -> "http://cdn/" + inv.getArgument(0));

        PerfilResponse res = service.obtenerPerfilPropio(usuarioId);

        assertThat(res).isNotNull();
        assertThat(res.usuarioId()).isEqualTo(usuarioId);
        assertThat(res.nombreArtista()).isEqualTo("Lucía Mora");
        assertThat(res.biografia()).isEqualTo("Mi bio");
        assertThat(res.esPropio()).isTrue();
    }

    @Test
    void usuarioNoExisteLanzaNotFound() {
        UUID noExiste = UUID.randomUUID();
        when(usuarioRepo.findById(noExiste)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPerfilPropio(noExiste))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Usuario no encontrado");
    }
}
