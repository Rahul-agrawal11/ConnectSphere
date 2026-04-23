package com.connectsphere.media.controller;

import com.connectsphere.media.dto.response.ApiResponse;
import com.connectsphere.media.dto.response.MediaResponse;
import com.connectsphere.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Media REST controller.
 * Handles file uploads (multipart/form-data) and media metadata queries.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "File upload and media metadata management")
public class MediaController {

    private final MediaService mediaService;

    // ── Upload ────────────────────────────────────────────────────────────

    @Operation(
            summary = "Upload a media file (image or video)",
            description = "Send as multipart/form-data. " +
                    "Supported: JPEG, PNG, WebP (images), MP4 (videos). " +
                    "linkedPostId is optional — set it to attach media to a post.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaResponse>> uploadMedia(
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long linkedPostId) {

        MediaResponse response = mediaService.uploadMedia(
                userId, file, linkedPostId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Media uploaded successfully", response));
    }

    // ── Read ──────────────────────────────────────────────────────────────

    @Operation(summary = "Get media by ID")
    @GetMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<MediaResponse>> getMedia(
            @PathVariable Long mediaId) {

        return ResponseEntity.ok(
                ApiResponse.success("Media fetched",
                        mediaService.getMediaById(mediaId)));
    }

    @Operation(summary = "Get all media for a specific post")
    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> getMediaByPost(
            @PathVariable Long postId) {

        return ResponseEntity.ok(
                ApiResponse.success("Media for post",
                        mediaService.getMediaByPost(postId)));
    }

    @Operation(
            summary = "Get all media uploaded by a user (paginated)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/uploader/{uploaderId}")
    public ResponseEntity<ApiResponse<Page<MediaResponse>>> getMediaByUploader(
            @PathVariable Long uploaderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Uploader media",
                        mediaService.getMediaByUploader(uploaderId, pageable)));
    }

    // ── Delete ────────────────────────────────────────────────────────────

    @Operation(
            summary = "Soft-delete a media record",
            description = "Marks media as deleted. Physical file is retained " +
                    "for 30 days per the audit trail policy.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable Long mediaId,
            @RequestHeader("X-User-Id") Long userId) {

        mediaService.deleteMedia(mediaId, userId);
        return ResponseEntity.ok(ApiResponse.success("Media deleted"));
    }

    @Operation(
            summary = "[Internal] Soft-delete all media for a post",
            description = "Called by post-service when a post is deleted."
    )
    @DeleteMapping("/post/{postId}/soft-delete")
    public ResponseEntity<Void> softDeleteByPost(@PathVariable Long postId) {
        mediaService.softDeleteMediaByPost(postId);
        return ResponseEntity.ok().build();
    }
}