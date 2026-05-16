package com.connectsphere.notification.service;

import com.connectsphere.notification.dto.request.BulkNotificationRequest;
import com.connectsphere.notification.dto.request.CreateNotificationRequest;
import com.connectsphere.notification.dto.response.NotificationResponse;
import com.connectsphere.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Notification service contract.
 */
public interface NotificationService {

    // Create
    NotificationResponse createNotification(CreateNotificationRequest request);
    void sendBulkNotification(BulkNotificationRequest request);

    // Read
    Page<NotificationResponse> getNotificationsByRecipient(
            Long recipientId, Pageable pageable);
    Page<NotificationResponse> getUnreadNotifications(
            Long recipientId, Pageable pageable);
    Page<NotificationResponse> getNotificationsByType(
            Long recipientId, NotificationType type, Pageable pageable);
    long getUnreadCount(Long recipientId);

    // Update read state
    void markAsRead(Long notificationId, Long recipientId);
    int markAllAsRead(Long recipientId);

    // Delete
    void deleteNotification(Long notificationId, Long recipientId);
    void deleteAllNotifications(Long recipientId);

    // Email
    void sendEmailAlert(String toEmail, String subject, String body);
}