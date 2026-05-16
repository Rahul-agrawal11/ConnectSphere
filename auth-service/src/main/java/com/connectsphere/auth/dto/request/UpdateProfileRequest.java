package com.connectsphere.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for PUT /api/v1/auth/profile
 * All fields optional — only provided fields are updated.
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    private String profilePicUrl;
}