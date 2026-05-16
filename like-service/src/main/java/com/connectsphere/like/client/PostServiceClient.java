package com.connectsphere.like.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
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

    /**
     * Returns the owner (author) ID of the given post.
     * Used by like-service to know who to notify.
     */
    @GetMapping("/{postId}/owner")
    Long getPostOwnerId(@PathVariable("postId") Long postId);
}