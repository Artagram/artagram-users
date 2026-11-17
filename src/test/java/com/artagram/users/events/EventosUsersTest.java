package com.artagram.users.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.users.domain.Perfil;
import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.UsoCuenta;
import com.artagram.users.domain.UsoCuentaRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class EventosUsersTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PerfilRepository perfiles = mock(PerfilRepository.class);
    private final UsoCuentaRepository usos = mock(UsoCuentaRepository.class);
    private final UsersEventListener listener = new UsersEventListener(usuarios, perfiles, usos);
    private final UUID id = UUID.randomUUID();

    @Test
    void registroCreaUsuarioPerfilYCuotas() {
        when(usuarios.findById(id)).thenReturn(Optional.empty());
        when(usuarios.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(perfiles.findByUsuarioId(id)).thenReturn(Optional.empty());
        when(usos.findByUsuarioId(id)).thenReturn(Optional.empty());

        listener.onUserRegistered(Map.of("usuarioId", id.toString(), "nombreArtista", "ana", "correo", "a@c.com", "premium", true));

        verify(usuarios).save(any(Usuario.class));
        verify(perfiles).save(any(Perfil.class));
        ArgumentCaptor<UsoCuenta> uso = ArgumentCaptor.forClass(UsoCuenta.class);
        verify(usos).save(uso.capture());
        assertThat(uso.getValue().getMaxMegabytes()).isEqualTo(5120);
    }

    @Test
    void registroDeUnUsuarioExistenteNoDuplica() {
        Usuario u = new Usuario(id, "a", "A", "a@c.com", false);
        when(usuarios.findById(id)).thenReturn(Optional.of(u));
        when(perfiles.findByUsuarioId(id)).thenReturn(Optional.of(new Perfil(UUID.randomUUID(), u, "bio", null)));
        when(usos.findByUsuarioId(id)).thenReturn(Optional.of(new UsoCuenta()));

        listener.onUserRegistered(Map.of("usuarioId", id.toString(), "nombreArtista", "a"));
        listener.onUserRegistered(Map.of("usuarioId", "mal"));

        verify(usuarios, never()).save(any());
        verify(perfiles, never()).save(any());
        verify(usos, never()).save(any());
    }

    @Test
    void eliminacionYSuscripcion() {
        Usuario u = new Usuario(id, "a", "A", "a@c.com", false);
        when(usuarios.findById(id)).thenReturn(Optional.of(u));

        listener.onUserDeleted(Map.of("usuarioId", id.toString()));
        listener.onUserDeleted(Map.of("usuarioId", "mal"));
        listener.onSubscriptionUpdated(Map.of("usuarioId", id.toString(), "premium", true));
        listener.onSubscriptionUpdated(Map.of("usuarioId", "mal"));

        verify(usuarios).deleteById(id);
        assertThat(u.isPremium()).isTrue();
        verify(usuarios).save(u);
    }

    @Test
    void publicadorYConfiguracion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        UsersEventPublisher pub = new UsersEventPublisher(rabbit);
        UUID otro = UUID.randomUUID();

        pub.publicarFollowCreado(id, "ana", otro);
        pub.publicarFollowEliminado(id, otro);
        pub.publicarSuscripcionActualizada(id, false, "GRATIS");

        verify(rabbit).convertAndSend(eq("artagram.events"), eq("follow.created"), any(Object.class));
        verify(rabbit).convertAndSend(eq("artagram.events"), eq("follow.deleted"), any(Object.class));
        verify(rabbit).convertAndSend(eq("artagram.events"), eq("subscription.updated"), any(Object.class));

        UsersRabbitMqConfig c = new UsersRabbitMqConfig();
        var ex = c.eventsExchange();
        assertThat(c.jsonMessageConverter()).isNotNull();
        assertThat(c.usersUserRegisteredBinding(c.usersUserRegisteredQueue(), ex).getRoutingKey()).isEqualTo("user.registered");
        assertThat(c.usersUserDeletedBinding(c.usersUserDeletedQueue(), ex).getRoutingKey()).isEqualTo("user.deleted");
        assertThat(c.usersSubscriptionUpdatedBinding(c.usersSubscriptionUpdatedQueue(), ex).getRoutingKey())
                .isEqualTo("subscription.updated");
    }
}
