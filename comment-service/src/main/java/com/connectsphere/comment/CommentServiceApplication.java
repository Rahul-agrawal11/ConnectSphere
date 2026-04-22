package com.connectsphere.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ConnectSphere Comment Service
 *
 * Handles threaded comments and replies on posts.
 * Calls post-service via OpenFeign to keep commentsCount accurate.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class CommentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApplication.class, args);
    }

}
