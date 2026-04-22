package com.connectsphere.comment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Comment data returned to clients.
 *
 * isReply is derived: true when parentCommentId is non-null.
 * content is replaced with "[deleted]" for soft-deleted comments
 * so thread structure is preserved in the UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {

    private Long id;
    private Long postId;
    private Long authorId;
    private Long parentCommentId;
    private String content;
    private Integer likesCount;
    private Boolean isReply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}