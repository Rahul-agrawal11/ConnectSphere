package com.connectsphere.media.service.impl;

import com.connectsphere.media.dto.response.MediaResponse;
import com.connectsphere.media.dto.response.StoryResponse;
import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.enums.MediaType;
import com.connectsphere.media.exception.MediaNotFoundException;
import com.connectsphere.media.exception.StoryNotFoundException;
import com.connectsphere.media.exception.UnauthorizedActionException;
import com.connectsphere.media.repository.MediaRepository;
import com.connectsphere.media.repository.StoryRepository;
import com.connectsphere.media.service.MediaService;
import com.connectsphere.media.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaRepository mediaRepository;
    private final StoryRepository storyRepository;
    private final StorageService storageService;

    @Value("${app.storage.local.base-url:http://localhost:8087/files}")
    private String baseUrl;

    @Value("${app.story.expiry-hours:24}")
    private int storyExpiryHours;

    // Allowed MIME types
    private static final List<String> ALLOWED_IMAGE_TYPES =
            Arrays.asList("image/jpeg", "image/png", "image/webp");
    private static final List<String> ALLOWED_VIDEO_TYPES =
            List.of("video/mp4");

    // ── Media Upload and Management ──────────────────────────────────────

    @Override
    @Transactional
    public MediaResponse uploadMedia(Long uploaderId, MultipartFile file,
                                     Long linkedPostId) {
        validateFile(file);

        String mimeType = file.getContentType();
        MediaType mediaType = resolveMediaType(mimeType);
        String folder = mediaType == MediaType.IMAGE ? "images" : "videos";

        StorageService.StorageResult result =
                storageService.store(file, folder);

        Media media = Media.builder()
                .uploaderId(uploaderId)
                .url(result.url())
                .storageKey(result.storageKey())
                .mediaType(mediaType)
                .sizeKb(file.getSize() / 1024)
                .mimeType(mimeType)
                .originalFilename(file.getOriginalFilename())
                .linkedPostId(linkedPostId)
                .build();

        Media saved = mediaRepository.save(media);

        log.info("Media uploaded: id={} type={} uploaderId={}",
                saved.getId(), mediaType, uploaderId);

        return mapToMediaResponse(saved);
    }

    @Override
    public MediaResponse getMediaById(Long mediaId) {
        return mapToMediaResponse(findActiveMedia(mediaId));
    }

    @Override
    public List<MediaResponse> getMediaByPost(Long postId) {
        return mediaRepository
                .findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc(postId)
                .stream()
                .map(this::mapToMediaResponse)
                .toList();
    }

    @Override
    public Page<MediaResponse> getMediaByUploader(Long uploaderId,
                                                  Pageable pageable) {
        return mediaRepository
                .findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(
                        uploaderId, pageable)
                .map(this::mapToMediaResponse);
    }

    @Override
    @Transactional
    public void deleteMedia(Long mediaId, Long requesterId) {
        Media media = findActiveMedia(mediaId);

        if (!media.getUploaderId().equals(requesterId)) {
            throw new UnauthorizedActionException(
                    "You are not allowed to delete this media.");
        }

        media.setIsDeleted(true);
        mediaRepository.save(media);

        log.info("Media soft-deleted: id={} by uploaderId={}",
                mediaId, requesterId);
    }

    @Override
    @Transactional
    public void softDeleteMediaByPost(Long postId) {
        List<Media> mediaList = mediaRepository
                .findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc(postId);

        mediaList.forEach(m -> m.setIsDeleted(true));
        mediaRepository.saveAll(mediaList);

        log.info("Soft-deleted {} media records for postId={}",
                mediaList.size(), postId);
    }

    // ── Story Management ─────────────────────────────────────────────────

    @Override
    @Transactional
    public StoryResponse createStory(Long authorId, MultipartFile file,
                                     String caption) {
        validateFile(file);

        String mimeType = file.getContentType();
        MediaType mediaType = resolveMediaType(mimeType);
        String folder = "stories";

        StorageService.StorageResult result =
                storageService.store(file, folder);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(storyExpiryHours);

        Story story = Story.builder()
                .authorId(authorId)
                .mediaUrl(result.url())
                .caption(caption)
                .mediaType(mediaType)
                .expiresAt(expiresAt)
                .build();

        Story saved = storyRepository.save(story);

        log.info("Story created: id={} authorId={} expiresAt={}",
                saved.getId(), authorId, expiresAt);

        return mapToStoryResponse(saved);
    }

    @Override
    public StoryResponse getStoryById(Long storyId) {
        Story story = storyRepository.findByIdAndIsActiveTrue(storyId)
                .orElseThrow(() -> new StoryNotFoundException(
                        "Story not found or has expired: " + storyId));
        return mapToStoryResponse(story);
    }

    @Override
    public List<StoryResponse> getActiveStoriesByUser(Long authorId) {
        return storyRepository
                .findByAuthorIdAndIsActiveTrueOrderByCreatedAtDesc(authorId)
                .stream()
                .map(this::mapToStoryResponse)
                .toList();
    }

    @Override
    public List<StoryResponse> getStoriesFeed(List<Long> followedUserIds) {
        if (followedUserIds == null || followedUserIds.isEmpty()) {
            return List.of();
        }
        return storyRepository
                .findActiveStoriesByAuthorIds(followedUserIds)
                .stream()
                .map(this::mapToStoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public StoryResponse viewStory(Long storyId, Long viewerId) {
        Story story = storyRepository.findByIdAndIsActiveTrue(storyId)
                .orElseThrow(() -> new StoryNotFoundException(
                        "Story not found or has expired: " + storyId));

        // Don't count author's own views
        if (!story.getAuthorId().equals(viewerId)) {
            int updated = storyRepository.incrementViewsCount(storyId);
            if (updated > 0) {
                story.setViewsCount(story.getViewsCount() + 1);
            }
        }

        return mapToStoryResponse(story);
    }

    @Override
    @Transactional
    public void deleteStory(Long storyId, Long requesterId) {
        Story story = storyRepository.findByIdAndIsActiveTrue(storyId)
                .orElseThrow(() -> new StoryNotFoundException(
                        "Story not found or has expired: " + storyId));

        if (!story.getAuthorId().equals(requesterId)) {
            throw new UnauthorizedActionException(
                    "You are not allowed to delete this story.");
        }

        story.setIsActive(false);
        storyRepository.save(story);

        log.info("Story deleted: id={} by authorId={}", storyId, requesterId);
    }

    // ── Scheduled Expiry ─────────────────────────────────────────────────

    @Override
    @Transactional
    public int expireOldStories() {
        int expired = storyRepository.expireStories(LocalDateTime.now());
        if (expired > 0) {
            log.info("Story expiry job: expired {} stories", expired);
        }
        return expired;
    }

    // ── Private Helpers ──────────────────────────────────────────────────

    private Media findActiveMedia(Long mediaId) {
        return mediaRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new MediaNotFoundException(
                        "Media not found: " + mediaId));
    }

    /**
     * Validate file is not empty and has an allowed MIME type.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File must not be null or empty.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null) {
            throw new IllegalArgumentException(
                    "Could not determine file type.");
        }

        boolean isAllowed = ALLOWED_IMAGE_TYPES.contains(mimeType)
                || ALLOWED_VIDEO_TYPES.contains(mimeType);

        if (!isAllowed) {
            throw new IllegalArgumentException(
                    "File type not allowed: " + mimeType +
                            ". Allowed types: JPEG, PNG, WebP, MP4.");
        }
    }

    /**
     * Resolve MediaType enum from MIME type string.
     */
    private MediaType resolveMediaType(String mimeType) {
        if (mimeType != null && mimeType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        return MediaType.IMAGE;
    }

    private MediaResponse mapToMediaResponse(Media media) {
        return MediaResponse.builder()
                .id(media.getId())
                .uploaderId(media.getUploaderId())
                .url(media.getUrl())
                .mediaType(media.getMediaType().name())
                .sizeKb(media.getSizeKb())
                .mimeType(media.getMimeType())
                .originalFilename(media.getOriginalFilename())
                .linkedPostId(media.getLinkedPostId())
                .uploadedAt(media.getUploadedAt())
                .build();
    }

    private StoryResponse mapToStoryResponse(Story story) {
        long secondsUntilExpiry = 0L;
        if (story.getIsActive() && story.getExpiresAt() != null) {
            secondsUntilExpiry = Math.max(0L,
                    ChronoUnit.SECONDS.between(
                            LocalDateTime.now(), story.getExpiresAt()));
        }

        return StoryResponse.builder()
                .id(story.getId())
                .authorId(story.getAuthorId())
                .mediaUrl(story.getMediaUrl())
                .caption(story.getCaption())
                .mediaType(story.getMediaType().name())
                .viewsCount(story.getViewsCount())
                .expiresAt(story.getExpiresAt())
                .createdAt(story.getCreatedAt())
                .secondsUntilExpiry(secondsUntilExpiry)
                .build();
    }
}