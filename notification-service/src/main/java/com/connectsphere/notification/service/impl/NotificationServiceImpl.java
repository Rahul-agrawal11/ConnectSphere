package com.connectsphere.notification.service.impl;

import com.connectsphere.notification.config.MailConfig;
import com.connectsphere.notification.dto.request.BulkNotificationRequest;
import com.connectsphere.notification.dto.request.CreateNotificationRequest;
import com.connectsphere.notification.dto.response.NotificationResponse;
import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.enums.NotificationType;
import com.connectsphere.notification.exception.NotificationNotFoundException;
import com.connectsphere.notification.repository.NotificationRepository;
import com.connectsphere.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final MailConfig mailConfig;

    // ── Create ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public NotificationResponse createNotification(
            CreateNotificationRequest request) {

        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .actorId(request.getActorId())
                .type(request.getType())
                .message(request.getMessage())
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .deepLinkUrl(request.getDeepLinkUrl())
                .build();

        Notification saved = notificationRepository.save(notification);

        log.info("Notification created: id={} type={} recipientId={}",
                saved.getId(), saved.getType(), saved.getRecipientId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void sendBulkNotification(BulkNotificationRequest request) {

        List<Notification> notifications = request.getRecipientIds()
                .stream()
                .map(recipientId -> Notification.builder()
                        .recipientId(recipientId)
                        .actorId(null) // system-generated
                        .type(request.getType())
                        .message(request.getMessage())
                        .targetId(request.getTargetId())
                        .targetType(request.getTargetType())
                        .deepLinkUrl(request.getDeepLinkUrl())
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);

        log.info("Bulk notification sent: type={} recipientCount={}",
                request.getType(), request.getRecipientIds().size());
    }

    // ── Read ─────────────────────────────────────────────────────────────

    @Override
    public Page<NotificationResponse> getNotificationsByRecipient(
            Long recipientId, Pageable pageable) {

        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<NotificationResponse> getUnreadNotifications(
            Long recipientId, Pageable pageable) {

        return notificationRepository
                .findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(
                        recipientId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<NotificationResponse> getNotificationsByType(
            Long recipientId, NotificationType type, Pageable pageable) {

        return notificationRepository
                .findByRecipientIdAndTypeOrderByCreatedAtDesc(
                        recipientId, type, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public long getUnreadCount(Long recipientId) {
        return notificationRepository
                .countByRecipientIdAndIsReadFalse(recipientId);
    }

    // ── Update Read State ────────────────────────────────────────────────

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long recipientId) {

        // Verify ownership before marking read
        if (!notificationRepository.existsByIdAndRecipientId(
                notificationId, recipientId)) {
            throw new NotificationNotFoundException(
                    "Notification not found: " + notificationId);
        }

        int updated = notificationRepository.markAsRead(
                notificationId, recipientId);

        if (updated == 0) {
            log.debug("Notification {} was already read for recipient {}",
                    notificationId, recipientId);
        }
    }

    @Override
    @Transactional
    public int markAllAsRead(Long recipientId) {
        int updated = notificationRepository.markAllAsRead(recipientId);
        log.info("Marked {} notifications as read for recipientId={}",
                updated, recipientId);
        return updated;
    }

    // ── Delete ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long recipientId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification not found: " + notificationId));

        // Ensure the notification belongs to the requesting user
        if (!notification.getRecipientId().equals(recipientId)) {
            throw new NotificationNotFoundException(
                    "Notification not found: " + notificationId);
        }

        notificationRepository.delete(notification);
        log.info("Notification deleted: id={} recipientId={}",
                notificationId, recipientId);
    }

    @Override
    @Transactional
    public void deleteAllNotifications(Long recipientId) {
        notificationRepository.deleteAllByRecipientId(recipientId);
        log.info("All notifications deleted for recipientId={}", recipientId);
    }

    // ── Email Alert ──────────────────────────────────────────────────────

    /**
     * Send an email alert asynchronously.
     * @Async ensures the calling thread is not blocked by SMTP latency.
     * Requires @EnableAsync on the application class.
     *
     * In development, app.mail.enabled=false skips sending entirely.
     * In production: set enabled=true and configure SMTP credentials.
     */
    @Override
    @Async
    public void sendEmailAlert(String toEmail, String subject, String body) {

        if (!mailConfig.isMailEnabled()) {
            log.debug("Email sending disabled — skipping alert to: {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailConfig.getFromAddress());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email alert sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ── Private Helpers ──────────────────────────────────────────────────

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .actorId(notification.getActorId())
                .type(notification.getType().name())
                .message(notification.getMessage())
                .targetId(notification.getTargetId())
                .targetType(notification.getTargetType() != null
                        ? notification.getTargetType().name() : null)
                .deepLinkUrl(notification.getDeepLinkUrl())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}