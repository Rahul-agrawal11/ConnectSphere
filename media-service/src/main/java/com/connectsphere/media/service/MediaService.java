package com.connectsphere.media.service;

import com.connectsphere.media.dto.response.MediaResponse;
import com.connectsphere.media.dto.response.StoryResponse;
import com.connectsphere.media.enums.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Media service contract.
 */
public interface MediaService {

    // Media upload and management
    MediaResponse uploadMedia(Long uploaderId, MultipartFile file,
                              Long linkedPostId);
    MediaResponse getMediaById(Long mediaId);
    List<MediaResponse> getMediaByPost(Long postId);
    Page<MediaResponse> getMediaByUploader(Long uploaderId, Pageable pageable);
    void deleteMedia(Long mediaId, Long requesterId);
    void softDeleteMediaByPost(Long postId);

    // Story management
    StoryResponse createStory(Long authorId, MultipartFile file,
                              String caption);
    StoryResponse getStoryById(Long storyId, Long requesterId);
    List<StoryResponse> getActiveStoriesByUser(Long authorId, Long requesterId);
    List<StoryResponse> getStoriesFeed(List<Long> followedUserIds, Long requesterId);
    StoryResponse viewStory(Long storyId, Long viewerId);
    void deleteStory(Long storyId, Long requesterId);

    // Scheduled expiry — called by StoryExpiryScheduler
    int expireOldStories();
}