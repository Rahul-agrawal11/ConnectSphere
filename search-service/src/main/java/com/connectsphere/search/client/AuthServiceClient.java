package com.connectsphere.search.client;

import com.connectsphere.search.dto.response.UserSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for auth-service.
 *
 * Used to search users by username or full name.
 * auth-service owns all user identity data — search-service
 * delegates user search entirely to it.
 *
 * Returns the raw response object — the controller maps it
 * to UserSummaryResponse for the client.
 */
@FeignClient(name = "auth-service", path = "/api/v1/auth")
public interface AuthServiceClient {

    /**
     * Search users by username or full name.
     * auth-service.searchUsers() returns a list of UserProfileResponse.
     */
    @GetMapping("/search")
    Object searchUsers(@RequestParam("query") String query);
}