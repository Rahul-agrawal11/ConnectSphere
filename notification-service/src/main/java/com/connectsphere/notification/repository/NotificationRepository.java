package com.connectsphere.notification.repository;

import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // All notifications for a recipient — newest first
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(
            Long recipientId, Pageable pageable);

    // Unread notifications for a recipient — newest first
    Page<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(
            Long recipientId, Pageable pageable);

    // Unread count — used for the badge
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    // Notifications by type for a recipient
    Page<Notification> findByRecipientIdAndTypeOrderByCreatedAtDesc(
            Long recipientId, NotificationType type, Pageable pageable);

    // Mark a single notification as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.id = :id AND n.recipientId = :recipientId")
    int markAsRead(
            @Param("id") Long id,
            @Param("recipientId") Long recipientId);

    // Mark ALL notifications for a recipient as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllAsRead(@Param("recipientId") Long recipientId);

    // Delete all notifications for a recipient (bulk clean)
    @Modifying
    @Query("DELETE FROM Notification n " +
            "WHERE n.recipientId = :recipientId")
    void deleteAllByRecipientId(@Param("recipientId") Long recipientId);

    // Check if a notification belongs to a recipient (ownership check)
    boolean existsByIdAndRecipientId(Long id, Long recipientId);
}