package com.connectsphere.notification.event;

import com.connectsphere.notification.dto.request.CreateNotificationRequest;
import com.connectsphere.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;

import java.io.IOException;

/**
 * RabbitMQ message listener for notification events.
 *
 * Listens on connectsphere.notification.queue for events published
 * by other services (like-service, comment-service, follow-service).
 *
 * Uses manual acknowledgement:
 *   - basicAck on success → message removed from queue
 *   - basicNack with requeue=false on failure → message goes to DLQ
 *
 * This prevents message loss while avoiding infinite retry loops.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(
            queues = "${app.rabbitmq.queues.notification}",
            ackMode = "MANUAL"
    )
    public void handleNotificationEvent(
            NotificationEvent event,
            Channel channel,
            Message message) {

        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("Received notification event: type={} recipientId={}",
                    event.getType(), event.getRecipientId());

            // Build request from event and delegate to service
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .recipientId(event.getRecipientId())
                    .actorId(event.getActorId())
                    .type(event.getType())
                    .message(event.getMessage())
                    .targetId(event.getTargetId())
                    .targetType(event.getTargetType())
                    .deepLinkUrl(event.getDeepLinkUrl())
                    .build();

            notificationService.createNotification(request);

            // Acknowledge — message processed successfully
            channel.basicAck(deliveryTag, false);

            log.debug("Notification event acknowledged: deliveryTag={}",
                    deliveryTag);

        } catch (Exception e) {
            log.error("Failed to process notification event: {} — {}",
                    event, e.getMessage(), e);

            try {
                // Nack without requeue → message goes to DLQ
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioEx) {
                log.error("Failed to nack message: {}", ioEx.getMessage());
            }
        }
    }
}