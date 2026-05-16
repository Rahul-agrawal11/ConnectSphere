package com.connectsphere.media.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response for the "who viewed my story" endpoint.
 * Contains the viewer list plus the total count.
 */
@Data
@Builder
public class StoryViewersResponse {

    /** Story ID this response belongs to */
    private Long storyId;

    /** Total unique viewers */
    private long totalViewers;

    /** Ordered list of viewers (newest first) — limited by page size */
    private List<ViewerEntry> viewers;

    @Data
    @Builder
    public static class ViewerEntry {
        private Long viewerId;
        private LocalDateTime viewedAt;
    }
}