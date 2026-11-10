package com.artagram.users.kpi;

import com.artagram.users.service.ActividadEnVivoService;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convierte los eventos minimos de login/sesion-colaborativa en filas de
 * eventos_negocio. Las sesiones tambien alimentan "dibujando ahora" desde aqui:
 * dos listeners sobre una misma cola se repartirian los mensajes.
 */
@Component
public class KpiEventListener {

    private static final Logger log = LoggerFactory.getLogger(KpiEventListener.class);

    private final EventoNegocioRepository repository;
    private final ActividadEnVivoService actividadEnVivo;

    public KpiEventListener(EventoNegocioRepository repository, ActividadEnVivoService actividadEnVivo) {
        this.repository = repository;
        this.actividadEnVivo = actividadEnVivo;
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
            if (payload.get("actividadId") != null) {
                actividadEnVivo.iniciar(String.valueOf(payload.get("actividadId")), usuarioId,
                        String.valueOf(payload.get("tipo")), UUID.fromString(String.valueOf(payload.get("recursoId"))));
            }
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
            if (payload.get("actividadId") != null) {
                actividadEnVivo.terminar(String.valueOf(payload.get("actividadId")));
            }
        } catch (Exception e) {
            log.error("Error procesando collab.session.ended: {}", e.getMessage());
        }
    }
}
