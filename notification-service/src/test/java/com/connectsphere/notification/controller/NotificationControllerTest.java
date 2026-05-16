package com.connectsphere.notification.controller;

import com.connectsphere.notification.dto.request.BulkNotificationRequest;
import com.connectsphere.notification.dto.request.CreateNotificationRequest;
import com.connectsphere.notification.dto.response.NotificationResponse;
import com.connectsphere.notification.enums.NotificationTargetType;
import com.connectsphere.notification.enums.NotificationType;
import com.connectsphere.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class NotificationControllerTest {

    private MockMvc mockMvc;
    private NotificationService notificationService;
    private ObjectMapper objectMapper;
    private NotificationResponse notificationResponse;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        objectMapper = new ObjectMapper();
        mockMvc = standaloneSetup(new NotificationController(notificationService)).build();

        notificationResponse = NotificationResponse.builder()
                .id(1L)
                .recipientId(10L)
                .actorId(20L)
                .type("LIKE")
                .message("Rahul liked your post")
                .targetId(100L)
                .targetType("POST")
                .deepLinkUrl("/posts/100")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createNotification_ShouldReturnCreatedNotification() throws Exception {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientId(10L)
                .actorId(20L)
                .type(NotificationType.LIKE)
                .message("Rahul liked your post")
                .targetId(100L)
                .targetType(NotificationTargetType.POST)
                .deepLinkUrl("/posts/100")
                .build();

        when(notificationService.createNotification(any(CreateNotificationRequest.class)))
                .thenReturn(notificationResponse);

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification created"))
                .andExpect(jsonPath("$.data.type").value("LIKE"));
    }

    @Test
    void sendBulkNotification_ShouldReturnForbidden_WhenRoleIsNotAdmin() throws Exception {
        BulkNotificationRequest request = BulkNotificationRequest.builder()
                .recipientIds(List.of(10L, 20L))
                .type(NotificationType.BROADCAST)
                .message("System maintenance")
                .targetType(NotificationTargetType.SYSTEM)
                .build();

        mockMvc.perform(post("/api/v1/notifications/bulk")
                        .header("X-User-Role", "USER")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Admin access required"));

        verify(notificationService, never()).sendBulkNotification(any());
    }

    @Test
    void sendBulkNotification_ShouldDispatch_WhenRoleIsAdmin() throws Exception {
        BulkNotificationRequest request = BulkNotificationRequest.builder()
                .recipientIds(List.of(10L, 20L))
                .type(NotificationType.BROADCAST)
                .message("System maintenance")
                .targetType(NotificationTargetType.SYSTEM)
                .build();

        mockMvc.perform(post("/api/v1/notifications/bulk")
                        .header("X-User-Role", "ADMIN")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Bulk notification dispatched to 2 users"));

        verify(notificationService).sendBulkNotification(any(BulkNotificationRequest.class));
    }

    @Test
    void getNotifications_ShouldReturnPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<NotificationResponse> page =
                new PageImpl<>(List.of(notificationResponse), pageable, 1);

        when(notificationService.getNotificationsByRecipient(eq(10L), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications")
                        .header("X-User-Id", 10L)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notifications fetched"))
                .andExpect(jsonPath("$.data.content[0].message").value("Rahul liked your post"));
    }

    @Test
    void getUnread_ShouldReturnUnreadPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<NotificationResponse> page =
                new PageImpl<>(List.of(notificationResponse), pageable, 1);

        when(notificationService.getUnreadNotifications(eq(10L), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications/unread")
                        .header("X-User-Id", 10L)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Unread notifications"))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false));
    }

    @Test
    void getByType_ShouldReturnTypedNotifications() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<NotificationResponse> page =
                new PageImpl<>(List.of(notificationResponse), pageable, 1);

        when(notificationService.getNotificationsByType(
                eq(10L), eq(NotificationType.LIKE), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications/type/LIKE")
                        .header("X-User-Id", 10L)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notifications by type"))
                .andExpect(jsonPath("$.data.content[0].type").value("LIKE"));
    }

    @Test
    void getUnreadCount_ShouldReturnCount() throws Exception {
        when(notificationService.getUnreadCount(10L)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/notifications/unread/count")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Unread count"))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void markAsRead_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/1/read")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification marked as read"));

        verify(notificationService).markAsRead(1L, 10L);
    }

    @Test
    void markAllAsRead_ShouldReturnUpdatedCount() throws Exception {
        when(notificationService.markAllAsRead(10L)).thenReturn(3);

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("3 notifications marked as read"))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void deleteNotification_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification deleted"));

        verify(notificationService).deleteNotification(1L, 10L);
    }

    @Test
    void deleteAllNotifications_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/all")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications deleted"));

        verify(notificationService).deleteAllNotifications(10L);
    }
}