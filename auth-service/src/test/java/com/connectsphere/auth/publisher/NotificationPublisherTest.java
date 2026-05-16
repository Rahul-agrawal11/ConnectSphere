package com.connectsphere.auth.publisher;

import com.connectsphere.auth.dto.event.OtpEmailEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherTest {

    @Mock
    AmqpTemplate amqpTemplate;

    @InjectMocks
    NotificationPublisher notificationPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationPublisher, "exchange", "auth.exchange");
        ReflectionTestUtils.setField(notificationPublisher, "otpRoutingKeys", "otp.routing.key");
    }

    @Test
    void sendOtpEmail_shouldPublishEventWithCorrectFields() {
        notificationPublisher.sendOtpEmail("rahul@gmail.com", "Rahul", "123456");

        ArgumentCaptor<OtpEmailEvent> captor = ArgumentCaptor.forClass(OtpEmailEvent.class);
        verify(amqpTemplate).convertAndSend(
                eq("auth.exchange"), eq("otp.routing.key"), captor.capture());

        OtpEmailEvent event = captor.getValue();
        assertThat(event.getToEmail()).isEqualTo("rahul@gmail.com");
        assertThat(event.getUserName()).isEqualTo("Rahul");
        assertThat(event.getOtp()).isEqualTo("123456");
        assertThat(event.getType()).isEqualTo("OTP_EMAIL");
    }
}