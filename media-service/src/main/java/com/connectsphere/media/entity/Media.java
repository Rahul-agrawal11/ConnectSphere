package com.connectsphere.media.entity;

import com.connectsphere.media.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Media entity — stores metadata for uploaded files.
 *
 * The actual file bytes live in local filesystem (dev) or AWS S3 (prod).
 * This entity stores only the URL and metadata.
 *
 * linkedPostId links this media record to a post.
 * Null means the media was uploaded but not yet attached to a post
 * (upload-first, attach-later flow).
 *
 * isDeleted = true → soft-deleted when the associated post is deleted.
 * Retained for 30 days per the audit trail NFR.
 */
@Entity
@Table(
        name = "media",
        indexes = {
                @Index(name = "idx_media_uploader",  columnList = "uploader_id"),
                @Index(name = "idx_media_post",      columnList = "linked_post_id"),
                @Index(name = "idx_media_type",      columnList = "media_type"),
                @Index(name = "idx_media_deleted",   columnList = "is_deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // References cs_auth_db.users.id — no FK (cross-service)
    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    // Public URL to access the file (CDN or local serve URL)
    @Column(nullable = false)
    private String url;

    // Storage key used to delete the file from S3 or local disk
    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaType mediaType;

    // File size in kilobytes
    @Column(name = "size_kb")
    private Long sizeKb;

    // MIME type: image/jpeg, image/png, video/mp4, etc.
    @Column(name = "mime_type", length = 50)
    private String mimeType;

    // Original filename as uploaded by the user
    @Column(name = "original_filename")
    private String originalFilename;

    // References cs_post_db.posts.id — no FK (cross-service)
    @Column(name = "linked_post_id")
    private Long linkedPostId;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}