package com.artagram.users.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UsersRabbitMqConfig {

    public static final String EXCHANGE = "artagram.events";
    public static final String DLX_EXCHANGE = "artagram.dlx";

    public static final String QUEUE_USER_REGISTERED = "users.user.registered";
    public static final String QUEUE_USER_DELETED = "users.user.deleted";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue usersUserRegisteredQueue() {
        return QueueBuilder.durable(QUEUE_USER_REGISTERED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_USER_REGISTERED + ".dlq")
                .build();
    }

    @Bean
    public Binding usersUserRegisteredBinding(Queue usersUserRegisteredQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(usersUserRegisteredQueue).to(eventsExchange).with("user.registered");
    }

    @Bean
    public Queue usersUserDeletedQueue() {
        return QueueBuilder.durable(QUEUE_USER_DELETED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_USER_DELETED + ".dlq")
                .build();
    }

    @Bean
    public Binding usersUserDeletedBinding(Queue usersUserDeletedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(usersUserDeletedQueue).to(eventsExchange).with("user.deleted");
    }
}
