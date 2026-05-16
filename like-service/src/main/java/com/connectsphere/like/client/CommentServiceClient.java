package com.connectsphere.like.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign client for comment-service.
 * Called when a user reacts to or removes a reaction from a COMMENT target.
 */
@FeignClient(name = "comment-service", path = "/api/v1/comments")
public interface CommentServiceClient {

    @PostMapping("/{commentId}/likes/increment")
    void incrementLikesCount(@PathVariable("commentId") Long commentId);

    @PostMapping("/{commentId}/likes/decrement")
    void decrementLikesCount(@PathVariable("commentId") Long commentId);

    /**
     * Returns the author ID of the given comment.
     * Used by like-service to know who to notify.
     */
    @GetMapping("/{commentId}/owner")
    Long getCommentOwnerId(@PathVariable("commentId") Long commentId);
}