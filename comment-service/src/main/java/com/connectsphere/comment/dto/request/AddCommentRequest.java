package com.connectsphere.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for POST /api/v1/comments
 *
 * parentCommentId is optional:
 *   null     → create a top-level comment on postId
 *   non-null → create a reply to that comment
 */
@Data
public class AddCommentRequest {

    @NotNull(message = "postId is required")
    private Long postId;

    // null for top-level comments, set for replies
    private Long parentCommentId;

    @NotBlank(message = "Comment content cannot be empty")
    @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
    private String content;
}