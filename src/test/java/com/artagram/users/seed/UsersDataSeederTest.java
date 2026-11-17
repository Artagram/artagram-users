package com.artagram.users.seed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.users.domain.ObraDestacadaRepository;
import com.artagram.users.domain.Perfil;
import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.SeguidorRepository;
import com.artagram.users.domain.Suscripcion;
import com.artagram.users.domain.SuscripcionRepository;
import com.artagram.users.domain.UsoCuenta;
import com.artagram.users.domain.UsoCuentaRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.storage.StorageService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class UsersDataSeederTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PerfilRepository perfiles = mock(PerfilRepository.class);
    private final ObraDestacadaRepository obras = mock(ObraDestacadaRepository.class);
    private final SeguidorRepository seguidores = mock(SeguidorRepository.class);
    private final SuscripcionRepository subs = mock(SuscripcionRepository.class);
    private final UsoCuentaRepository usos = mock(UsoCuentaRepository.class);
    private final StorageService storage = mock(StorageService.class);

    private UsersDataSeeder seeder() {
        return new UsersDataSeeder(usuarios, perfiles, obras, seguidores, subs, usos, storage, new DefaultResourceLoader());
    }

    @Test
    void siembraLosDoceUsuariosConPerfilCuotasYPlanes() {
        when(usuarios.findById(any())).thenReturn(Optional.empty());
        when(usuarios.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(perfiles.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storage.guardar(anyString(), anyString(), any(), anyString())).thenReturn("avatars/a.jpg");

        seeder().run();

        verify(usuarios, times(12)).save(any());
        verify(perfiles, times(12)).save(any());
        verify(obras, times(12)).save(any());
        verify(usos, times(12)).save(any());
        verify(subs, times(2)).save(any());
        verify(seguidores, times(2)).save(any());
    }

    @Test
    void conDatosExistentesNoDuplicaYToleraFallosDeAvatar() {
        when(usuarios.findById(any())).thenAnswer(inv -> Optional.of(new Usuario(inv.getArgument(0), "u", "U", "u@c.com", true)));
        when(perfiles.findByUsuarioId(any())).thenReturn(Optional.of(new Perfil()));
        when(usos.findByUsuarioId(any())).thenReturn(Optional.of(new UsoCuenta()));
        when(subs.findTopByUsuarioIdOrderByFechaInicioDesc(any())).thenReturn(Optional.of(new Suscripcion()));
        when(seguidores.existsById_SeguidorIdAndId_SeguidoId(any(), any())).thenReturn(true);

        seeder().run();

        verify(usuarios, never()).save(any());
        verify(perfiles, never()).save(any());
        verify(subs, never()).save(any());
        verify(seguidores, never()).save(any());

        UsuarioRepository otros = mock(UsuarioRepository.class);
        when(otros.findById(any())).thenReturn(Optional.empty());
        when(otros.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(perfiles.findByUsuarioId(any())).thenReturn(Optional.empty());
        when(perfiles.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storage.guardar(anyString(), anyString(), any(), anyString())).thenThrow(new IllegalStateException("lleno"));
        new UsersDataSeeder(otros, perfiles, obras, seguidores, subs, usos, storage, new DefaultResourceLoader()).run();
        verify(perfiles, times(12)).save(any());
    }
}
