package com.connectsphere.post.dto.request;

import com.connectsphere.post.enums.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request body for POST /api/v1/posts
 */
@Data
public class CreatePostRequest {

    @NotBlank(message = "Post content cannot be empty")
    @Size(max = 5000, message = "Post content cannot exceed 5000 characters")
    private String content;

    // Optional list of media URLs already uploaded via media-service
    private List<String> mediaUrls;

    // Defaults to PUBLIC if not provided
    private PostVisibility visibility = PostVisibility.PUBLIC;
}