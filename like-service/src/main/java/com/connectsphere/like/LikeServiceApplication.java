package com.connectsphere.like;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ConnectSphere Like Service
 *
 * Polymorphic reaction engine for posts and comments.
 * Supports six reaction types and enforces one reaction per user per target.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class LikeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LikeServiceApplication.class, args);
    }

}
