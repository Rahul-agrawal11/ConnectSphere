package com.connectsphere.notification.service;

import com.connectsphere.notification.config.MailConfig;
import com.connectsphere.notification.dto.request.BulkNotificationRequest;
import com.connectsphere.notification.dto.request.CreateNotificationRequest;
import com.connectsphere.notification.dto.response.NotificationResponse;
import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.enums.NotificationTargetType;
import com.connectsphere.notification.enums.NotificationType;
import com.connectsphere.notification.exception.NotificationNotFoundException;
import com.connectsphere.notification.repository.NotificationRepository;
import com.connectsphere.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailConfig mailConfig;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .id(1L)
                .recipientId(10L)
                .actorId(20L)
                .type(NotificationType.LIKE)
                .message("Rahul liked your post")
                .targetId(100L)
                .targetType(NotificationTargetType.POST)
                .deepLinkUrl("/posts/100")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createNotification_ShouldCreateNotificationSuccessfully() {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientId(10L)
                .actorId(20L)
                .type(NotificationType.LIKE)
                .message("Rahul liked your post")
                .targetId(100L)
                .targetType(NotificationTargetType.POST)
                .deepLinkUrl("/posts/100")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.createNotification(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getRecipientId());
        assertEquals(20L, response.getActorId());
        assertEquals("LIKE", response.getType());
        assertEquals("POST", response.getTargetType());
        assertFalse(response.getIsRead());

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void sendBulkNotification_ShouldSaveNotificationsForAllRecipients() {
        BulkNotificationRequest request = BulkNotificationRequest.builder()
                .recipientIds(List.of(10L, 20L, 30L))
                .type(NotificationType.BROADCAST)
                .message("System maintenance tonight")
                .targetId(null)
                .targetType(NotificationTargetType.SYSTEM)
                .deepLinkUrl(null)
                .build();

        notificationService.sendBulkNotification(request);

        verify(notificationRepository).saveAll(argThat(notifications -> {
            List<Notification> list = (List<Notification>) notifications;
            return list.size() == 3
                    && list.get(0).getType() == NotificationType.BROADCAST
                    && list.get(0).getActorId() == null;
        }));
    }

    @Test
    void getNotificationsByRecipient_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(10L, pageable))
                .thenReturn(page);

        Page<NotificationResponse> response =
                notificationService.getNotificationsByRecipient(10L, pageable);

        assertEquals(1, response.getContent().size());
        assertEquals("Rahul liked your post", response.getContent().get(0).getMessage());
    }

    @Test
    void getUnreadNotifications_ShouldReturnUnreadPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(10L, pageable))
                .thenReturn(page);

        Page<NotificationResponse> response =
                notificationService.getUnreadNotifications(10L, pageable);

        assertEquals(1, response.getContent().size());
        assertFalse(response.getContent().get(0).getIsRead());
    }

    @Test
    void getNotificationsByType_ShouldReturnFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByRecipientIdAndTypeOrderByCreatedAtDesc(
                10L, NotificationType.LIKE, pageable))
                .thenReturn(page);

        Page<NotificationResponse> response =
                notificationService.getNotificationsByType(10L, NotificationType.LIKE, pageable);

        assertEquals(1, response.getContent().size());
        assertEquals("LIKE", response.getContent().get(0).getType());
    }

    @Test
    void getUnreadCount_ShouldReturnCount() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse(10L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(10L);

        assertEquals(5L, count);
    }

    @Test
    void markAsRead_ShouldMarkNotificationAsRead_WhenNotificationBelongsToUser() {
        when(notificationRepository.existsByIdAndRecipientId(1L, 10L)).thenReturn(true);
        when(notificationRepository.markAsRead(1L, 10L)).thenReturn(1);

        notificationService.markAsRead(1L, 10L);

        verify(notificationRepository).markAsRead(1L, 10L);
    }

    @Test
    void markAsRead_ShouldThrowException_WhenNotificationDoesNotBelongToUser() {
        when(notificationRepository.existsByIdAndRecipientId(1L, 99L)).thenReturn(false);

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.markAsRead(1L, 99L));

        verify(notificationRepository, never()).markAsRead(anyLong(), anyLong());
    }

    @Test
    void markAllAsRead_ShouldReturnUpdatedCount() {
        when(notificationRepository.markAllAsRead(10L)).thenReturn(3);

        int count = notificationService.markAllAsRead(10L);

        assertEquals(3, count);
    }

    @Test
    void deleteNotification_ShouldDelete_WhenNotificationBelongsToUser() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(1L, 10L);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotification_ShouldThrowException_WhenNotificationNotFound() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.deleteNotification(1L, 10L));
    }

    @Test
    void deleteNotification_ShouldThrowException_WhenNotificationBelongsToOtherUser() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.deleteNotification(1L, 99L));

        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void deleteAllNotifications_ShouldDeleteAllByRecipient() {
        notificationService.deleteAllNotifications(10L);

        verify(notificationRepository).deleteAllByRecipientId(10L);
    }

    @Test
    void sendEmailAlert_ShouldNotSendEmail_WhenMailDisabled() {
        when(mailConfig.isMailEnabled()).thenReturn(false);

        notificationService.sendEmailAlert("user@gmail.com", "Test", "Body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmailAlert_ShouldSendEmail_WhenMailEnabled() {
        when(mailConfig.isMailEnabled()).thenReturn(true);
        when(mailConfig.getFromAddress()).thenReturn("noreply@connectsphere.com");

        notificationService.sendEmailAlert("user@gmail.com", "Test Subject", "Test Body");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmailAlert_ShouldCatchException_WhenMailSendingFails() {
        when(mailConfig.isMailEnabled()).thenReturn(true);
        when(mailConfig.getFromAddress()).thenReturn("noreply@connectsphere.com");
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() ->
                notificationService.sendEmailAlert("user@gmail.com", "Test", "Body"));
    }
}