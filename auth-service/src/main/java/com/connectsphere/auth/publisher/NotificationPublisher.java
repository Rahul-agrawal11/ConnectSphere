package com.connectsphere.auth.publisher;

import com.connectsphere.auth.dto.event.OtpEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final AmqpTemplate amqpTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-keys.otp}")
    private String otpRoutingKeys;

    public void sendOtpEmail(String toEmail, String userName, String otp) {
        OtpEmailEvent event = new OtpEmailEvent("OTP_EMAIL", toEmail, userName, otp);
        amqpTemplate.convertAndSend(exchange, otpRoutingKeys, event);
        log.info("OTP event published for: {}", toEmail);
    }
}
