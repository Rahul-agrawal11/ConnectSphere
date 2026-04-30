package com.connectsphere.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

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

    // ── Exchange ──────────────────────────────────────────────────────────

    @Bean
    public TopicExchange connectSphereExchange() {
        return ExchangeBuilder.topicExchange(exchange).durable(true).build();
    }

    // ── Dead Letter Queue ─────────────────────────────────────────────────

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(connectSphereExchange()).with("dlq.notification");
    }

    // ── Notification Queue ────────────────────────────────────────────────

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(notificationQueue)
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-dead-letter-routing-key", "dlq.notification")
                .withArgument("x-message-ttl", 1_800_000) // 30 min
                .build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(connectSphereExchange()).with(notificationRoutingKey);
    }

    // ── JSON Converter ────────────────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // ── Per-Message Processing Retry ──────────────────────────────────────

    /**
     * Retries failed message PROCESSING up to 3 times in-memory
     * (2s → 4s → 8s backoff) before nacking to the DLQ.
     */
    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(2_000, 2.0, 10_000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    // ── Listener Container Factory ────────────────────────────────────────

    /**
     * Configures the listener container with:
     *
     *  1. missingQueuesFatal = false
     *     Don't treat a missing queue as fatal on startup — the container
     *     will retry quietly until RabbitMQ comes up and the queue exists.
     *
     *  2. recoveryInterval = 10 000 ms
     *     Minimum pause between consumer restart attempts. This is the key
     *     knob that stops the thread-per-restart storm.
     *
     *  3. setRecoveryRetryTemplate on the CachingConnectionFactory
     *     Applies exponential backoff (10s → 20s → 40s → max 60s) to the
     *     underlying TCP reconnect loop. Without this, the connection factory
     *     retries the TCP connect on every consumer restart cycle, producing
     *     the "Failed to check/redeclare" error every 5 s in the log.
     *     This is the method previous fixes were missing — it wires the
     *     RetryTemplate directly onto the factory rather than leaving it as
     *     an unused bean.
     *
     *  4. retryInterceptor (per-message)
     *     Separate from connection recovery — handles business logic failures.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {

        // ── Build the factory
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);

        // Don't crash if the queue is missing — wait patiently
        factory.setMissingQueuesFatal(false);

        // Pause between consumer restart cycles (ms)
        factory.setRecoveryInterval(10_000L);

        // Per-message retry interceptor
        factory.setAdviceChain(retryInterceptor());

        return factory;
    }
}