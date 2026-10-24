package com.artagram.users.kpi;

import com.artagram.users.events.UsersRabbitMqConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Colas propias para los eventos minimos que alimentan los KPIs de negocio
 * (HU-37/38): login (artagram-auth) y entrada/salida de una sesion
 * colaborativa (artagram-canvas). El exchange y el MessageConverter ya los
 * declara {@link UsersRabbitMqConfig} — aqui solo se agregan las colas y sus
 * bindings, reutilizando ese mismo exchange.
 */
@Configuration
public class KpiEventsRabbitMqConfig {

    public static final String QUEUE_LOGIN = "users.user.login";
    public static final String QUEUE_SESSION_STARTED = "users.collab.session.started";
    public static final String QUEUE_SESSION_ENDED = "users.collab.session.ended";

    @Bean
    public Queue usersLoginQueue() {
        return QueueBuilder.durable(QUEUE_LOGIN)
                .deadLetterExchange(UsersRabbitMqConfig.DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_LOGIN + ".dlq")
                .build();
    }

    @Bean
    public Binding usersLoginBinding(Queue usersLoginQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(usersLoginQueue).to(eventsExchange).with("user.login");
    }

    @Bean
    public Queue usersCollabSessionStartedQueue() {
        return QueueBuilder.durable(QUEUE_SESSION_STARTED)
                .deadLetterExchange(UsersRabbitMqConfig.DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_SESSION_STARTED + ".dlq")
                .build();
    }

    @Bean
    public Binding usersCollabSessionStartedBinding(Queue usersCollabSessionStartedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(usersCollabSessionStartedQueue).to(eventsExchange).with("collab.session.started");
    }

    @Bean
    public Queue usersCollabSessionEndedQueue() {
        return QueueBuilder.durable(QUEUE_SESSION_ENDED)
                .deadLetterExchange(UsersRabbitMqConfig.DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_SESSION_ENDED + ".dlq")
                .build();
    }

    @Bean
    public Binding usersCollabSessionEndedBinding(Queue usersCollabSessionEndedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(usersCollabSessionEndedQueue).to(eventsExchange).with("collab.session.ended");
    }
}
