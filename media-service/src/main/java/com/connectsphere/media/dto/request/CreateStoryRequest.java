package com.connectsphere.media.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for POST /api/v1/stories
 *
 * The actual media file is sent as a multipart/form-data part.
 * This DTO carries the optional text caption alongside the file.
 */
@Data
public class CreateStoryRequest {

    @Size(max = 500, message = "Caption cannot exceed 500 characters")
    private String caption;
}