package com.connectsphere.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight user summary for search results.
 *
 * Populated by calling auth-service via Feign.
 * Only fields needed for user search result cards are included.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;
    private String username;
    private String fullName;
    private String profilePicUrl;
    private String bio;
}