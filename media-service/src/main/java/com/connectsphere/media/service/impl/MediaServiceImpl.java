package com.connectsphere.media.service.impl;

import com.connectsphere.media.dto.response.MediaResponse;
import com.connectsphere.media.dto.response.StoryResponse;
import com.connectsphere.media.dto.response.StoryViewersResponse;
import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.entity.StoryView;
import com.connectsphere.media.enums.MediaType;
import com.connectsphere.media.exception.MediaNotFoundException;
import com.connectsphere.media.exception.StoryNotFoundException;
import com.connectsphere.media.exception.UnauthorizedActionException;
import com.connectsphere.media.repository.MediaRepository;
import com.connectsphere.media.repository.StoryRepository;
import com.connectsphere.media.repository.StoryViewRepository;
import com.connectsphere.media.service.MediaService;
import com.connectsphere.media.service.StorageService;
import com.connectsphere.media.client.FollowServiceClient;
import com.connectsphere.media.event.NotificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final StoryViewRepository storyViewRepository;
    private final StorageService storageService;

    private final FollowServiceClient followServiceClient;
    private final RabbitTemplate rabbitTemplate;

    private static final String STORY_NOT_FOUND_MESSAGE =
            "Story not found or has expired: ";

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-keys.story}")
    private String storyRoutingKey;


    @Value("${app.storage.local.base-url:http://localhost:8087/files}")
    private String baseUrl;

    @Value("${app.story.expiry-hours:24}")
    private int storyExpiryHours;

    private static final List<String> ALLOWED_IMAGE_TYPES =
            Arrays.asList("image/jpeg", "image/png", "image/webp");
    private static final List<String> ALLOWED_VIDEO_TYPES =
            List.of("video/mp4");

    // ── Media Upload ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public MediaResponse uploadMedia(Long uploaderId, MultipartFile file,
                                     Long linkedPostId) {
        validateFile(file);
        String mimeType = file.getContentType();
        MediaType mediaType = resolveMediaType(mimeType);
        String folder = mediaType == MediaType.IMAGE ? "images" : "videos";
        StorageService.StorageResult result = storageService.store(file, folder);

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

        return mapToMediaResponse(mediaRepository.save(media));
    }

    @Override
    public MediaResponse getMediaById(Long mediaId) {
        return mapToMediaResponse(findActiveMedia(mediaId));
    }

    @Override
    public List<MediaResponse> getMediaByPost(Long postId) {
        return mediaRepository
                .findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc(postId)
                .stream().map(this::mapToMediaResponse).toList();
    }

    @Override
    public Page<MediaResponse> getMediaByUploader(Long uploaderId, Pageable pageable) {
        return mediaRepository
                .findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(uploaderId, pageable)
                .map(this::mapToMediaResponse);
    }

    @Override
    @Transactional
    public void deleteMedia(Long mediaId, Long requesterId) {
        Media media = findActiveMedia(mediaId);
        if (!media.getUploaderId().equals(requesterId)) {
            throw new UnauthorizedActionException("You are not allowed to delete this media.");
        }
        media.setIsDeleted(true);
        mediaRepository.save(media);
    }

    @Override
    @Transactional
    public void softDeleteMediaByPost(Long postId) {
        List<Media> list = mediaRepository
                .findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc(postId);
        list.forEach(m -> m.setIsDeleted(true));
        mediaRepository.saveAll(list);
    }

    // ── Story Management ──────────────────────────────────────────────────

    @Override
    @Transactional
    public StoryResponse createStory(Long authorId, MultipartFile file, String caption) {
        validateFile(file);
        String mimeType = file.getContentType();
        MediaType mediaType = resolveMediaType(mimeType);
        StorageService.StorageResult result = storageService.store(file, "stories");

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
        log.info("Story created: id={} authorId={} expiresAt={}", saved.getId(), authorId, expiresAt);

        // ── Notify all followers that a new story was posted ──────────────
        publishStoryNotificationsToFollowers(authorId, saved.getId());

        return mapToStoryResponse(saved, authorId);
    }


    @Override
    public StoryResponse getStoryById(Long storyId, Long requesterId) {
        Story story = storyRepository.findByIdAndIsActiveTrue(storyId)
                .orElseThrow(() -> new StoryNotFoundException(
                        STORY_NOT_FOUND_MESSAGE + storyId));
        return mapToStoryResponse(story, requesterId);
    }

    @Override
    public List<StoryResponse> getActiveStoriesByUser(Long authorId, Long requesterId) {
        return storyRepository
                .findByAuthorIdAndIsActiveTrueOrderByCreatedAtDesc(authorId)
                .stream()
                .map(s -> mapToStoryResponse(s, requesterId))
                .toList();
    }

    @Override
    public List<StoryResponse> getStoriesFeed(List<Long> followedUserIds, Long requesterId) {
        if (followedUserIds == null || followedUserIds.isEmpty()) return List.of();
        return storyRepository
                .findActiveStoriesByAuthorIds(followedUserIds)
                .stream()
                .map(s -> mapToStoryResponse(s, requesterId))
                .toList();
    }

    /**
     * View a story — records the viewer in story_views (unique per user per story)
     * and atomically increments the denormalised viewsCount on the story row.
     * <p>
     * FIX: Previously only incremented viewsCount (integer) with no record of WHO
     * viewed. Now inserts a StoryView row first; if the row already exists
     * (DataIntegrityViolationException on the unique constraint) the view is
     * silently ignored so the count stays accurate.
     */
    @Override
    @Transactional
    public StoryResponse viewStory(Long storyId, Long viewerId) {
        Story story = storyRepository.findByIdAndIsActiveTrue(storyId)
                .orElseThrow(() -> new StoryNotFoundException(
                        STORY_NOT_FOUND_MESSAGE + storyId));

        // Don't count author's own views
        if (!story.getAuthorId().equals(viewerId)) {
            boolean alreadyViewed = storyViewRepository
                    .existsByStoryIdAndViewerId(storyId, viewerId);

            if (!alreadyViewed) {
                try {
                    storyViewRepository.save(
                            StoryView.builder()
                                    .storyId(storyId)
                                    .viewerId(viewerId)
                                    .build()
                    );
                    // Increment counter atomically — avoids optimistic lock conflicts
                    storyRepository.incrementViewsCount(storyId);
                    story.setViewsCount(story.getViewsCount() + 1);
                } catch (DataIntegrityViolationException e) {
                    // Race condition: another request inserted the row first — ignore
                    log.debug("Duplicate story view ignored: storyId={} viewerId={}", storyId, viewerId);
                }
            }
        }

        return mapToStoryResponse(story, viewerId);
    }

    /**
     * Get the list of users who have viewed a story.
     * Only the story author can call this (enforced in controller via X-User-Id check).
     */
    @Override
    public StoryViewersResponse getStoryViewers(Long storyId, Long requesterId) {
        Story story = storyRepository.findByIdAndIsActiveTrue(storyId)
                .orElseThrow(() -> new StoryNotFoundException(
                        STORY_NOT_FOUND_MESSAGE + storyId));

        if (!story.getAuthorId().equals(requesterId)) {
            throw new UnauthorizedActionException(
                    "Only the story author can view who has seen this story.");
        }

        List<StoryView> views = storyViewRepository
                .findByStoryIdOrderByViewedAtDesc(storyId);

        List<StoryViewersResponse.ViewerEntry> entries = views.stream()
                .map(v -> StoryViewersResponse.ViewerEntry.builder()
                        .viewerId(v.getViewerId())
                        .viewedAt(v.getViewedAt())
                        .build())
                .toList();

        return StoryViewersResponse.builder()
                .storyId(storyId)
                .totalViewers(entries.size())
                .viewers(entries)
                .build();
    }

    @Override
    @Transactional
    public void deleteStory(Long storyId, Long requesterId) {
        Story story = storyRepository.findByIdAndIsActiveTrue(storyId)
                .orElseThrow(() -> new StoryNotFoundException(
                        STORY_NOT_FOUND_MESSAGE + storyId));
        if (!story.getAuthorId().equals(requesterId)) {
            throw new UnauthorizedActionException(
                    "You are not allowed to delete this story.");
        }
        story.setIsActive(false);
        storyRepository.save(story);
        log.info("Story deleted: id={} by authorId={}", storyId, requesterId);
    }

    @Override
    @Transactional
    public int expireOldStories() {
        int expired = storyRepository.expireStories(LocalDateTime.now());
        if (expired > 0) log.info("Story expiry job: expired {} stories", expired);
        return expired;
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private Media findActiveMedia(Long mediaId) {
        return mediaRepository.findByIdAndIsDeletedFalse(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found: " + mediaId));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File must not be null or empty.");
        String mimeType = file.getContentType();
        if (mimeType == null)
            throw new IllegalArgumentException("Could not determine file type.");
        boolean isAllowed = ALLOWED_IMAGE_TYPES.contains(mimeType)
                || ALLOWED_VIDEO_TYPES.contains(mimeType);
        if (!isAllowed)
            throw new IllegalArgumentException(
                    "File type not allowed: " + mimeType + ". Allowed: JPEG, PNG, WebP, MP4.");
    }

    private MediaType resolveMediaType(String mimeType) {
        return (mimeType != null && mimeType.startsWith("video/"))
                ? MediaType.VIDEO : MediaType.IMAGE;
    }

    private void publishStoryNotificationsToFollowers(Long authorId, Long storyId) {
        try {
            List<Long> followerIds = followServiceClient.getFollowerIds(authorId);
            if (followerIds == null || followerIds.isEmpty()) return;

            for (Long followerId : followerIds) {
                publishStoryNotificationToFollower(followerId, authorId, storyId);
            }

            log.info("Story notifications published: storyId={} authorId={} followers={}",
                    storyId, authorId, followerIds.size());

        } catch (Exception e) {
            log.error("Failed to fetch followers for story notification: {}", e.getMessage(), e);
        }
    }

    private MediaResponse mapToMediaResponse(Media m) {
        return MediaResponse.builder()
                .id(m.getId()).uploaderId(m.getUploaderId()).url(m.getUrl())
                .mediaType(m.getMediaType().name()).sizeKb(m.getSizeKb())
                .mimeType(m.getMimeType()).originalFilename(m.getOriginalFilename())
                .linkedPostId(m.getLinkedPostId()).uploadedAt(m.getUploadedAt())
                .build();
    }

    private void publishStoryNotificationToFollower(
            Long followerId,
            Long authorId,
            Long storyId) {

        try {
            NotificationEvent event = NotificationEvent.builder()
                    .recipientId(followerId)
                    .actorId(authorId)
                    .type("STORY")
                    .message("Someone you follow posted a new story.")
                    .targetId(storyId)
                    .targetType("STORY")
                    .build();

            rabbitTemplate.convertAndSend(
                    exchange,
                    storyRoutingKey,
                    event
            );
        } catch (Exception ex) {
            log.error("Failed to publish story notification to followerId={}",
                    followerId, ex);
        }
    }

    private StoryResponse mapToStoryResponse(Story story, Long requesterId) {
        long secondsUntilExpiry = 0L;
        if (story.getIsActive() && story.getExpiresAt() != null) {
            secondsUntilExpiry = Math.max(
                    0L, ChronoUnit.SECONDS.between(LocalDateTime.now(), story.getExpiresAt()));
        }
        boolean isOwner = requesterId != null && story.getAuthorId().equals(requesterId);
        return StoryResponse.builder()
                .id(story.getId()).authorId(story.getAuthorId())
                .mediaUrl(story.getMediaUrl()).caption(story.getCaption())
                .mediaType(story.getMediaType().name())
                .viewsCount(isOwner ? story.getViewsCount() : null)
                .expiresAt(story.getExpiresAt()).createdAt(story.getCreatedAt())
                .secondsUntilExpiry(secondsUntilExpiry)
                .build();
    }
}