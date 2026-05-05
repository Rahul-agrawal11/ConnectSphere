package com.connectsphere.notification.repository;

import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.enums.NotificationTargetType;
import com.connectsphere.notification.enums.NotificationType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    private Notification createNotification(
            Long recipientId,
            Long actorId,
            NotificationType type,
            boolean isRead
    ) {
        return Notification.builder()
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .message("Test notification")
                .targetId(100L)
                .targetType(NotificationTargetType.POST)
                .deepLinkUrl("/posts/100")
                .isRead(isRead)
                .build();
    }

    @Test
    @DisplayName("Should find notifications by recipient")
    void findByRecipientIdOrderByCreatedAtDesc_ShouldReturnNotifications() {
        notificationRepository.save(createNotification(10L, 20L, NotificationType.LIKE, false));
        notificationRepository.save(createNotification(10L, 21L, NotificationType.COMMENT, false));
        notificationRepository.save(createNotification(99L, 22L, NotificationType.FOLLOW, false));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Notification> result =
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(10L, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should find unread notifications by recipient")
    void findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc_ShouldReturnUnreadOnly() {
        notificationRepository.save(createNotification(10L, 20L, NotificationType.LIKE, false));
        notificationRepository.save(createNotification(10L, 21L, NotificationType.COMMENT, true));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Notification> result =
                notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(10L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsRead()).isFalse();
    }

    @Test
    @DisplayName("Should count unread notifications")
    void countByRecipientIdAndIsReadFalse_ShouldReturnCount() {
        notificationRepository.save(createNotification(10L, 20L, NotificationType.LIKE, false));
        notificationRepository.save(createNotification(10L, 21L, NotificationType.COMMENT, false));
        notificationRepository.save(createNotification(10L, 22L, NotificationType.FOLLOW, true));

        long count = notificationRepository.countByRecipientIdAndIsReadFalse(10L);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find notifications by recipient and type")
    void findByRecipientIdAndTypeOrderByCreatedAtDesc_ShouldReturnTypedNotifications() {
        notificationRepository.save(createNotification(10L, 20L, NotificationType.LIKE, false));
        notificationRepository.save(createNotification(10L, 21L, NotificationType.COMMENT, false));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Notification> result =
                notificationRepository.findByRecipientIdAndTypeOrderByCreatedAtDesc(
                        10L, NotificationType.LIKE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(NotificationType.LIKE);
    }

    @Test
    @DisplayName("Should check notification belongs to recipient")
    void existsByIdAndRecipientId_ShouldReturnTrue() {
        Notification saved =
                notificationRepository.save(createNotification(10L, 20L, NotificationType.LIKE, false));

        boolean exists = notificationRepository.existsByIdAndRecipientId(saved.getId(), 10L);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should mark notification as read")
    void markAsRead_ShouldUpdateReadStatus() {
        Notification saved =
                notificationRepository.saveAndFlush(createNotification(10L, 20L, NotificationType.LIKE, false));

        int updated = notificationRepository.markAsRead(saved.getId(), 10L);

        entityManager.clear();

        assertThat(updated).isEqualTo(1);

        Notification found = notificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("Should mark all notifications as read")
    void markAllAsRead_ShouldUpdateUnreadNotifications() {
        notificationRepository.save(createNotification(10L, 20L, NotificationType.LIKE, false));
        notificationRepository.save(createNotification(10L, 21L, NotificationType.COMMENT, false));
        notificationRepository.save(createNotification(10L, 22L, NotificationType.FOLLOW, true));
        notificationRepository.flush();

        int updated = notificationRepository.markAllAsRead(10L);

        entityManager.clear();

        assertThat(updated).isEqualTo(2);
        assertThat(notificationRepository.countByRecipientIdAndIsReadFalse(10L)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should delete all notifications by recipient")
    void deleteAllByRecipientId_ShouldDeleteRecipientNotifications() {
        notificationRepository.save(createNotification(10L, 20L, NotificationType.LIKE, false));
        notificationRepository.save(createNotification(10L, 21L, NotificationType.COMMENT, false));
        notificationRepository.save(createNotification(99L, 22L, NotificationType.FOLLOW, false));
        notificationRepository.flush();

        notificationRepository.deleteAllByRecipientId(10L);
        entityManager.clear();

        Page<Notification> result =
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                        10L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }
}