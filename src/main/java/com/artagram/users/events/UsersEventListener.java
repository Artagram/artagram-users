package com.artagram.users.events;

import com.artagram.users.domain.Perfil;
import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.UsoCuenta;
import com.artagram.users.domain.UsoCuentaRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UsersEventListener {

    private static final Logger log = LoggerFactory.getLogger(UsersEventListener.class);

    private final UsuarioRepository usuarioRepo;
    private final PerfilRepository perfilRepo;
    private final UsoCuentaRepository usoRepo;

    public UsersEventListener(UsuarioRepository usuarioRepo,
                              PerfilRepository perfilRepo,
                              UsoCuentaRepository usoRepo) {
        this.usuarioRepo = usuarioRepo;
        this.perfilRepo = perfilRepo;
        this.usoRepo = usoRepo;
    }

    @RabbitListener(queues = UsersRabbitMqConfig.QUEUE_USER_REGISTERED)
    @Transactional
    public void onUserRegistered(Map<String, Object> payload) {
        try {
            UUID id = UUID.fromString((String) payload.get("id"));
            String nombreArtista = (String) payload.get("nombreArtista");
            String correo = (String) payload.get("correo");
            Boolean premium = (Boolean) payload.get("premium");

            Usuario usuario = usuarioRepo.findById(id).orElseGet(() -> {
                Usuario u = new Usuario(id, nombreArtista, nombreArtista, correo, Boolean.TRUE.equals(premium));
                return usuarioRepo.save(u);
            });

            if (!perfilRepo.findByUsuarioId(id).isPresent()) {
                perfilRepo.save(new Perfil(UUID.randomUUID(), usuario, "Artista en ArtaGram", null));
            }
            if (!usoRepo.findByUsuarioId(id).isPresent()) {
                usoRepo.save(new UsoCuenta(UUID.randomUUID(), id, 0, usuario.isPremium() ? 5120 : 512, 0, usuario.isPremium() ? 500 : 50));
            }
            log.info("Usuario y perfil creados/sincronizados en artagram-users para id={}", id);
        } catch (Exception ex) {
            log.error("Error al procesar user.registered en artagram-users: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = UsersRabbitMqConfig.QUEUE_USER_DELETED)
    @Transactional
    public void onUserDeleted(Map<String, Object> payload) {
        try {
            UUID id = UUID.fromString((String) payload.get("id"));
            usuarioRepo.deleteById(id);
            log.info("Usuario eliminado en artagram-users para id={}", id);
        } catch (Exception ex) {
            log.error("Error al procesar user.deleted en artagram-users: {}", ex.getMessage());
        }
    }
}
