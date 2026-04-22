package com.connectsphere.comment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign client for post-service.
 *
 * Used to increment/decrement commentsCount on a post whenever
 * a comment is added or deleted.
 *
 * 'name' must match spring.application.name of post-service exactly.
 * Eureka resolves the actual host:port via load balancing.
 *
 * fallback = PostServiceClientFallback.class can be added later
 * when circuit breaker (Resilience4j) is integrated.
 */
@FeignClient(name = "post-service", path = "/api/v1/posts")
public interface PostServiceClient {

    @PostMapping("/{postId}/comments/increment")
    void incrementCommentsCount(@PathVariable("postId") Long postId);

    @PostMapping("/{postId}/comments/decrement")
    void decrementCommentsCount(@PathVariable("postId") Long postId);
}