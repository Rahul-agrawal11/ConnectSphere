package com.connectsphere.media.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign client for follow-service.
 * Used by media-service to fetch the follower list when a story is uploaded,
 * so each follower can be notified.
 */
@FeignClient(name = "follow-service", path = "/api/v1/follows")
public interface FollowServiceClient {

    @GetMapping("/internal/follower-ids/{userId}")
    List<Long> getFollowerIds(@PathVariable("userId") Long userId);
}