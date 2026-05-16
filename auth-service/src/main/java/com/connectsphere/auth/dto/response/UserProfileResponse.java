package com.connectsphere.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User profile data safe for public/authenticated API responses.
 * Never exposes passwordHash, providerId, or internal fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private String profilePicUrl;
    private String role;
    private String provider;
    private String status;
    private LocalDateTime createdAt;
}