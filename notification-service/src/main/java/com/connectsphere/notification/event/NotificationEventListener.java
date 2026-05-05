package com.connectsphere.notification.event;

import com.connectsphere.notification.dto.OtpEmailEvent;
import com.connectsphere.notification.dto.request.CreateNotificationRequest;
import com.connectsphere.notification.service.EmailService;
import com.connectsphere.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${app.rabbitmq.queues.notification}")
    public void handleNotificationEvent(Message rawMessage, Channel channel) {

        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        String body = new String(rawMessage.getBody());

        try {
            log.info("Raw message received: {}", body);

            // ── OTP Email ──────────────────────────────────────────────
            if (body.contains("\"type\":\"OTP_EMAIL\"")) {
                OtpEmailEvent otpEvent = objectMapper.readValue(body, OtpEmailEvent.class);
                log.info("OTP event received for: {}", otpEvent.getToEmail());

                emailService.sentOtpEmail(
                        otpEvent.getToEmail(),
                        otpEvent.getUserName(),
                        otpEvent.getOtp()
                );

                channel.basicAck(deliveryTag, false);
                log.info("✅ OTP email sent & ack'd for: {}", otpEvent.getToEmail());
                return;
            }

            // ── Social Notification ────────────────────────────────────
            NotificationEvent event = objectMapper.readValue(body, NotificationEvent.class);
            log.info("Social notification: type={} recipientId={}",
                    event.getType(), event.getRecipientId());

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
            channel.basicAck(deliveryTag, false);
            log.info("✅ Social notification ack'd: deliveryTag={}", deliveryTag);

        } catch (Exception e) {
            log.error("❌ Failed to process message: {}", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioEx) {
                log.error("Failed to nack: {}", ioEx.getMessage());
            }
        }
    }
}