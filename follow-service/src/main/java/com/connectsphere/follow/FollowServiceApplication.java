package com.connectsphere.follow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ConnectSphere Follow Service
 *
 * Manages the directed social graph — who follows whom.
 * Exposes getFollowingIds used by post-service for feed generation.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class FollowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FollowServiceApplication.class, args);
    }

}
