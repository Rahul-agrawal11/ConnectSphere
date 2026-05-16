package com.connectsphere.comment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "post-service", path = "/api/v1/posts")
public interface PostServiceClient {

    @PostMapping("/{postId}/comments/increment")
    void incrementCommentsCount(@PathVariable("postId") Long postId);

    @PostMapping("/{postId}/comments/decrement")
    void decrementCommentsCount(@PathVariable("postId") Long postId);

    /**
     * Returns the author (owner) ID of the given post.
     * Used to know who to send a COMMENT notification to.
     */
    @GetMapping("/{postId}/owner")
    Long getPostOwnerId(@PathVariable("postId") Long postId);
}