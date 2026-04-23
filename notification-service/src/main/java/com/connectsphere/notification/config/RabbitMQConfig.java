package com.connectsphere.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for ConnectSphere notification events.
 *
 * Topology:
 *
 *   Producers (like-service, comment-service, follow-service)
 *     │
 *     ▼
 *   [connectsphere.events] ← Topic Exchange
 *     │
 *     ├── routing key: notification.#
 *     │     ▼
 *     │   [connectsphere.notification.queue]
 *     │     │ on failure (nack, no requeue)
 *     │     ▼
 *     │   [connectsphere.notification.dlq]
 *     │
 *     └── (future: search.# → search-service queue)
 *
 * Topic exchange with routing key "notification.#" means any key
 * starting with "notification." is routed to the notification queue.
 * This allows fine-grained routing: notification.like, notification.follow etc.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queues.notification}")
    private String notificationQueue;

    @Value("${app.rabbitmq.queues.dead-letter}")
    private String deadLetterQueue;

    @Value("${app.rabbitmq.routing-keys.notification}")
    private String notificationRoutingKey;

    // ── Exchange ─────────────────────────────────────────────────────────

    @Bean
    public TopicExchange connectSphereExchange() {
        return ExchangeBuilder
                .topicExchange(exchange)
                .durable(true)
                .build();
    }

    // ── Dead Letter Queue ─────────────────────────────────────────────────

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(deadLetterQueue)
                .build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(connectSphereExchange())
                .with("dlq.notification");
    }

    // ── Notification Queue ────────────────────────────────────────────────

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder
                .durable(notificationQueue)
                // Failed messages route to DLQ
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-dead-letter-routing-key", "dlq.notification")
                // Optional: set TTL for messages (30 minutes)
                .withArgument("x-message-ttl", 1800000)
                .build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(connectSphereExchange())
                .with(notificationRoutingKey);
    }

    // ── Jackson JSON Message Converter ────────────────────────────────────

    /**
     * Use Jackson to serialize/deserialize AMQP messages as JSON.
     * This replaces the default Java serialization — safer and portable.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configure RabbitTemplate with JSON converter.
     * Other services use this template to publish events.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Configure listener container factory with JSON converter
     * and manual acknowledgement mode.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }
}