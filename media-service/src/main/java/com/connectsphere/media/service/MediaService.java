package com.connectsphere.media.service;

import com.connectsphere.media.dto.response.MediaResponse;
import com.connectsphere.media.dto.response.StoryResponse;
import com.connectsphere.media.dto.response.StoryViewersResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * MediaService — contract for all media upload and story lifecycle operations.
 */
public interface MediaService {

    // ── Media ─────────────────────────────────────────────────────────────
    MediaResponse   uploadMedia(Long uploaderId, MultipartFile file, Long linkedPostId);
    MediaResponse   getMediaById(Long mediaId);
    List<MediaResponse> getMediaByPost(Long postId);
    Page<MediaResponse> getMediaByUploader(Long uploaderId, Pageable pageable);
    void            deleteMedia(Long mediaId, Long requesterId);
    void            softDeleteMediaByPost(Long postId);

    // ── Stories ───────────────────────────────────────────────────────────
    StoryResponse   createStory(Long authorId, MultipartFile file, String caption);
    StoryResponse   getStoryById(Long storyId, Long requesterId);
    List<StoryResponse> getActiveStoriesByUser(Long authorId, Long requesterId);
    List<StoryResponse> getStoriesFeed(List<Long> followedUserIds, Long requesterId);

    /**
     * Record a story view and increment the view counter.
     * Author's own views are silently ignored.
     * Duplicate views by the same user are also silently ignored.
     */
    StoryResponse   viewStory(Long storyId, Long viewerId);

    /**
     * Return the list of users who have viewed a story.
     * Only callable by the story's author (enforced in the service layer).
     */
    StoryViewersResponse getStoryViewers(Long storyId, Long requesterId);

    void            deleteStory(Long storyId, Long requesterId);
    int             expireOldStories();
}