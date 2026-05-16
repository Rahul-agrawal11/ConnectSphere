package com.connectsphere.post.dto.request;

import com.connectsphere.post.enums.PostVisibility;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request body for PUT /api/v1/posts/{postId}
 * All fields optional — only provided fields are updated.
 */
@Data
public class UpdatePostRequest {

    @Size(max = 5000, message = "Post content cannot exceed 5000 characters")
    private String content;

    private List<String> mediaUrls;

    private PostVisibility visibility;
}