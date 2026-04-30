package com.connectsphere.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ConnectSphere Notification Service
 *
 * Stores, dispatches, and manages in-app and email notifications.
 * Consumes events from RabbitMQ and accepts direct REST calls.
 *
 * Exclusions:
 *  - UserDetailsServiceAutoConfiguration: this service sits behind the
 *    API Gateway and has no need for Spring Security's in-memory user store.
 *    Excluding it removes the "Using generated security password" warning.
 *
 *  - LoadBalancerAutoConfiguration: the notification-service does not make
 *    outbound Feign/RestClient calls to other services. Excluding the
 *    LoadBalancer auto-config removes the BeanPostProcessorChecker WARN lines
 *    and the Caffeine cache suggestion that appear on every startup.
 */
@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableAsync
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}