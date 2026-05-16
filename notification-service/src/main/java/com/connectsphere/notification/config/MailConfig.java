package com.connectsphere.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Mail configuration holder.
 *
 * JavaMailSender is auto-configured by Spring Boot from
 * spring.mail.* properties. This class holds app-level
 * mail settings (from address, enabled flag).
 *
 * To enable email alerts:
 *   1. Set app.mail.enabled=true in application.yml
 *   2. Configure spring.mail.username and spring.mail.password
 *      (use Gmail App Password, not your main password)
 *   3. Update app.mail.from to your sender address
 */
@Configuration
public class MailConfig {

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@connectsphere.com}")
    private String fromAddress;

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    public String getFromAddress() {
        return fromAddress;
    }
}