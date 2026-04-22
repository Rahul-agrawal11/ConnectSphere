package com.connectsphere.like.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign client for post-service.
 * Called when a user reacts to or removes a reaction from a POST target.
 */
@FeignClient(name = "post-service", path = "/api/v1/posts")
public interface PostServiceClient {

    @PostMapping("/{postId}/likes/increment")
    void incrementLikesCount(@PathVariable("postId") Long postId);

    @PostMapping("/{postId}/likes/decrement")
    void decrementLikesCount(@PathVariable("postId") Long postId);
}