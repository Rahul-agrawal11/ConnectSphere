package com.connectsphere.media.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Media metadata returned to clients.
 * Never exposes storageKey (internal detail) or isDeleted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {

    private Long id;
    private Long uploaderId;
    private String url;
    private String mediaType;
    private Long sizeKb;
    private String mimeType;
    private String originalFilename;
    private Long linkedPostId;
    private LocalDateTime uploadedAt;
}