package com.artagram.users.kpi;

import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Convierte los eventos minimos de login/sesion-colaborativa en filas de eventos_negocio. */
@Component
public class KpiEventListener {

    private static final Logger log = LoggerFactory.getLogger(KpiEventListener.class);

    private final EventoNegocioRepository repository;

    public KpiEventListener(EventoNegocioRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = KpiEventsRabbitMqConfig.QUEUE_LOGIN)
    @Transactional
    public void onLogin(Map<String, Object> payload) {
        try {
            UUID usuarioId = UUID.fromString(String.valueOf(payload.get("usuarioId")));
            repository.save(new EventoNegocio(TipoEventoNegocio.LOGIN, usuarioId, null));
        } catch (Exception e) {
            log.error("Error procesando user.login: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = KpiEventsRabbitMqConfig.QUEUE_SESSION_STARTED)
    @Transactional
    public void onSesionIniciada(Map<String, Object> payload) {
        try {
            UUID usuarioId = UUID.fromString(String.valueOf(payload.get("usuarioId")));
            repository.save(new EventoNegocio(TipoEventoNegocio.ENTRO_COLABORATIVO, usuarioId, null));
        } catch (Exception e) {
            log.error("Error procesando collab.session.started: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = KpiEventsRabbitMqConfig.QUEUE_SESSION_ENDED)
    @Transactional
    public void onSesionTerminada(Map<String, Object> payload) {
        try {
            UUID usuarioId = UUID.fromString(String.valueOf(payload.get("usuarioId")));
            long duracionMs = ((Number) payload.get("duracionMs")).longValue();
            repository.save(new EventoNegocio(TipoEventoNegocio.SALIO_COLABORATIVO, usuarioId, duracionMs));
        } catch (Exception e) {
            log.error("Error procesando collab.session.ended: {}", e.getMessage());
        }
    }
}
