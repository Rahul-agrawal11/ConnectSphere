package com.connectsphere.post.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "search-service")
public interface SearchServiceClient {

    @PostMapping("/api/v1/hashtags/index")
    void indexPost(@RequestParam Long postId,
                   @RequestParam String content);

    @DeleteMapping("/api/v1/hashtags/index/{postId}")
    void removePostIndex(@PathVariable Long postId);
}