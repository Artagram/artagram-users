package com.artagram.users.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class UsersEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UsersEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public UsersEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarFollowCreado(UUID seguidorId, UUID seguidoId) {
        Map<String, Object> evento = Map.of(
                "tipo", "FOLLOW_CREADO",
                "seguidorId", seguidorId.toString(),
                "seguidoId", seguidoId.toString(),
                "timestamp", Instant.now().toString()
        );
        log.info("Publicando follow.created: {} -> {}", seguidorId, seguidoId);
        rabbitTemplate.convertAndSend(UsersRabbitMqConfig.EXCHANGE, "follow.created", evento);
    }

    public void publicarFollowEliminado(UUID seguidorId, UUID seguidoId) {
        Map<String, Object> evento = Map.of(
                "tipo", "FOLLOW_ELIMINADO",
                "seguidorId", seguidorId.toString(),
                "seguidoId", seguidoId.toString(),
                "timestamp", Instant.now().toString()
        );
        log.info("Publicando follow.deleted: {} -> {}", seguidorId, seguidoId);
        rabbitTemplate.convertAndSend(UsersRabbitMqConfig.EXCHANGE, "follow.deleted", evento);
    }

    public void publicarSuscripcionActualizada(UUID usuarioId, boolean premium, String plan) {
        Map<String, Object> evento = Map.of(
                "tipo", "SUSCRIPCION_ACTUALIZADA",
                "usuarioId", usuarioId.toString(),
                "premium", premium,
                "plan", plan,
                "timestamp", Instant.now().toString()
        );
        log.info("Publicando subscription.updated para usuarioId={}, plan={}", usuarioId, plan);
        rabbitTemplate.convertAndSend(UsersRabbitMqConfig.EXCHANGE, "subscription.updated", evento);
    }
}
