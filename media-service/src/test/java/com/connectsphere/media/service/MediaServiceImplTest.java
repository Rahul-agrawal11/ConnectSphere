package com.connectsphere.media.service;

import com.connectsphere.media.client.FollowServiceClient;
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
import com.connectsphere.media.service.StorageService;
import com.connectsphere.media.service.impl.MediaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock MediaRepository mediaRepository;
    @Mock StoryRepository storyRepository;
    @Mock StoryViewRepository storyViewRepository;
    @Mock StorageService storageService;
    @Mock FollowServiceClient followServiceClient;
    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks
    MediaServiceImpl service;

    @BeforeEach
    void injectValues() {
        ReflectionTestUtils.setField(service, "exchange", "media.exchange");
        ReflectionTestUtils.setField(service, "storyRoutingKey", "story.routing.key");
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8087/files");
        ReflectionTestUtils.setField(service, "storyExpiryHours", 24);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private MockMultipartFile imageFile() {
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes());
    }

    private MockMultipartFile videoFile() {
        return new MockMultipartFile("file", "clip.mp4", "video/mp4", "data".getBytes());
    }

    private MockMultipartFile invalidFile() {
        return new MockMultipartFile("file", "doc.pdf", "application/pdf", "data".getBytes());
    }

    private Media buildMedia(Long id, Long uploaderId) {
        return Media.builder()
                .id(id).uploaderId(uploaderId)
                .url("http://localhost:8087/files/images/photo.jpg")
                .storageKey("images/photo.jpg")
                .mediaType(MediaType.IMAGE)
                .sizeKb(10L).mimeType("image/jpeg")
                .originalFilename("photo.jpg")
                .linkedPostId(100L)
                .isDeleted(false)
                .build();
    }

    private Story buildStory(Long id, Long authorId) {
        return Story.builder()
                .id(id).authorId(authorId)
                .mediaUrl("http://localhost:8087/files/stories/story.jpg")
                .mediaType(MediaType.IMAGE)
                .viewsCount(0)
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }

    // ── uploadMedia ───────────────────────────────────────────────────────

    @Test
    void uploadMedia_image_shouldSaveAndReturnResponse() {
        StorageService.StorageResult result =
                new StorageService.StorageResult("http://localhost:8087/files/images/photo.jpg", "images/photo.jpg");
        when(storageService.store(any(), eq("images"))).thenReturn(result);
        Media saved = buildMedia(1L, 10L);
        when(mediaRepository.save(any())).thenReturn(saved);

        MediaResponse response = service.uploadMedia(10L, imageFile(), 100L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getMediaType()).isEqualTo("IMAGE");
        verify(mediaRepository).save(any());
    }

    @Test
    void uploadMedia_video_shouldResolveVideoMediaType() {
        StorageService.StorageResult result =
                new StorageService.StorageResult("http://localhost:8087/files/videos/clip.mp4", "videos/clip.mp4");
        when(storageService.store(any(), eq("videos"))).thenReturn(result);
        Media saved = Media.builder().id(2L).uploaderId(10L)
                .url(result.url()).storageKey(result.storageKey())
                .mediaType(MediaType.VIDEO).sizeKb(500L).mimeType("video/mp4")
                .originalFilename("clip.mp4").isDeleted(false).build();
        when(mediaRepository.save(any())).thenReturn(saved);

        MediaResponse response = service.uploadMedia(10L, videoFile(), null);

        assertThat(response.getMediaType()).isEqualTo("VIDEO");
    }

    @Test
    void uploadMedia_invalidMimeType_shouldThrow() {
        assertThatThrownBy(() -> service.uploadMedia(10L, invalidFile(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    void uploadMedia_emptyFile_shouldThrow() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> service.uploadMedia(10L, empty, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File must not be null or empty");
    }

    @Test
    void uploadMedia_nullContentType_shouldThrow() {
        MockMultipartFile noType = new MockMultipartFile("file", "x.jpg", null, "data".getBytes());
        assertThatThrownBy(() -> service.uploadMedia(10L, noType, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Could not determine file type");
    }

    // ── getMediaById ──────────────────────────────────────────────────────

    @Test
    void getMediaById_existingMedia_shouldReturnResponse() {
        when(mediaRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(buildMedia(1L, 10L)));

        MediaResponse response = service.getMediaById(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getMediaById_notFound_shouldThrow() {
        when(mediaRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMediaById(999L))
                .isInstanceOf(MediaNotFoundException.class)
                .hasMessageContaining("Media not found: 999");
    }

    // ── getMediaByPost ────────────────────────────────────────────────────

    @Test
    void getMediaByPost_shouldReturnList() {
        when(mediaRepository.findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc(100L))
                .thenReturn(List.of(buildMedia(1L, 10L)));

        List<MediaResponse> responses = service.getMediaByPost(100L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getLinkedPostId()).isEqualTo(100L);
    }

    @Test
    void getMediaByPost_noMedia_shouldReturnEmptyList() {
        when(mediaRepository.findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc(999L))
                .thenReturn(List.of());

        assertThat(service.getMediaByPost(999L)).isEmpty();
    }

    // ── getMediaByUploader ────────────────────────────────────────────────

    @Test
    void getMediaByUploader_shouldReturnPage() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Media> page = new PageImpl<>(List.of(buildMedia(1L, 10L)), pageable, 1);
        when(mediaRepository.findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(10L, pageable))
                .thenReturn(page);

        Page<MediaResponse> result = service.getMediaByUploader(10L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── deleteMedia ───────────────────────────────────────────────────────

    @Test
    void deleteMedia_byOwner_shouldSoftDelete() {
        Media media = buildMedia(1L, 10L);
        when(mediaRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(media));

        service.deleteMedia(1L, 10L);

        assertThat(media.getIsDeleted()).isTrue();
        verify(mediaRepository).save(media);
    }

    @Test
    void deleteMedia_byNonOwner_shouldThrow() {
        Media media = buildMedia(1L, 10L);
        when(mediaRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(media));

        assertThatThrownBy(() -> service.deleteMedia(1L, 99L))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("not allowed to delete");
    }

    @Test
    void deleteMedia_mediaNotFound_shouldThrow() {
        when(mediaRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteMedia(1L, 10L))
                .isInstanceOf(MediaNotFoundException.class);
    }

    // ── softDeleteMediaByPost ─────────────────────────────────────────────

    @Test
    void softDeleteMediaByPost_shouldMarkAllDeleted() {
        Media m1 = buildMedia(1L, 10L);
        Media m2 = buildMedia(2L, 10L);
        when(mediaRepository.findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc(100L))
                .thenReturn(List.of(m1, m2));

        service.softDeleteMediaByPost(100L);

        assertThat(m1.getIsDeleted()).isTrue();
        assertThat(m2.getIsDeleted()).isTrue();
        verify(mediaRepository).saveAll(List.of(m1, m2));
    }

    // ── createStory ───────────────────────────────────────────────────────

    @Test
    void createStory_shouldSaveAndNotifyFollowers() {
        StorageService.StorageResult result =
                new StorageService.StorageResult("http://localhost:8087/files/stories/s.jpg", "stories/s.jpg");
        when(storageService.store(any(), eq("stories"))).thenReturn(result);
        Story saved = buildStory(1L, 10L);
        when(storyRepository.save(any())).thenReturn(saved);
        when(followServiceClient.getFollowerIds(10L)).thenReturn(List.of(20L, 30L));

        StoryResponse response = service.createStory(10L, imageFile(), "caption");

        assertThat(response.getId()).isEqualTo(1L);
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), Optional.ofNullable(any()));
    }

    @Test
    void createStory_withNoFollowers_shouldNotPublishEvents() {
        StorageService.StorageResult result =
                new StorageService.StorageResult("http://localhost:8087/files/stories/s.jpg", "stories/s.jpg");
        when(storageService.store(any(), eq("stories"))).thenReturn(result);
        when(storyRepository.save(any())).thenReturn(buildStory(1L, 10L));
        when(followServiceClient.getFollowerIds(10L)).thenReturn(List.of());

        service.createStory(10L, imageFile(), null);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), Optional.ofNullable(any()));
    }

    @Test
    void createStory_followerFetchFails_shouldNotThrow() {
        StorageService.StorageResult result =
                new StorageService.StorageResult("http://localhost:8087/files/stories/s.jpg", "stories/s.jpg");
        when(storageService.store(any(), eq("stories"))).thenReturn(result);
        when(storyRepository.save(any())).thenReturn(buildStory(1L, 10L));
        when(followServiceClient.getFollowerIds(any())).thenThrow(new RuntimeException("service down"));

        // Should not propagate the exception — story creation must succeed
        assertThatNoException().isThrownBy(() -> service.createStory(10L, imageFile(), null));
    }

    @Test
    void createStory_invalidFile_shouldThrow() {
        assertThatThrownBy(() -> service.createStory(10L, invalidFile(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── getStoryById ──────────────────────────────────────────────────────

    @Test
    void getStoryById_asOwner_shouldExposeViewsCount() {
        Story story = buildStory(1L, 10L);
        story.setViewsCount(5);
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(story));

        StoryResponse response = service.getStoryById(1L, 10L);

        assertThat(response.getViewsCount()).isEqualTo(5);
    }

    @Test
    void getStoryById_asViewer_shouldHideViewsCount() {
        Story story = buildStory(1L, 10L);
        story.setViewsCount(5);
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(story));

        StoryResponse response = service.getStoryById(1L, 99L);

        assertThat(response.getViewsCount()).isNull();
    }

    @Test
    void getStoryById_notFound_shouldThrow() {
        when(storyRepository.findByIdAndIsActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStoryById(999L, 10L))
                .isInstanceOf(StoryNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── getActiveStoriesByUser ────────────────────────────────────────────

    @Test
    void getActiveStoriesByUser_shouldReturnMappedList() {
        when(storyRepository.findByAuthorIdAndIsActiveTrueOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(buildStory(1L, 10L), buildStory(2L, 10L)));

        List<StoryResponse> responses = service.getActiveStoriesByUser(10L, 10L);

        assertThat(responses).hasSize(2);
    }

    // ── getStoriesFeed ────────────────────────────────────────────────────

    @Test
    void getStoriesFeed_withFollowedIds_shouldReturnFeed() {
        when(storyRepository.findActiveStoriesByAuthorIds(List.of(10L, 20L)))
                .thenReturn(List.of(buildStory(1L, 10L)));

        List<StoryResponse> feed = service.getStoriesFeed(List.of(10L, 20L), 99L);

        assertThat(feed).hasSize(1);
    }

    @Test
    void getStoriesFeed_emptyFollowList_shouldReturnEmpty() {
        assertThat(service.getStoriesFeed(List.of(), 99L)).isEmpty();
        verifyNoInteractions(storyRepository);
    }

    @Test
    void getStoriesFeed_nullFollowList_shouldReturnEmpty() {
        assertThat(service.getStoriesFeed(null, 99L)).isEmpty();
    }

    // ── viewStory ─────────────────────────────────────────────────────────

    @Test
    void viewStory_newViewer_shouldIncrementCount() {
        Story story = buildStory(1L, 10L);
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(story));
        when(storyViewRepository.existsByStoryIdAndViewerId(1L, 99L)).thenReturn(false);

        service.viewStory(1L, 99L);

        verify(storyViewRepository).save(any());
        verify(storyRepository).incrementViewsCount(1L);
    }

    @Test
    void viewStory_alreadyViewed_shouldNotIncrementAgain() {
        Story story = buildStory(1L, 10L);
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(story));
        when(storyViewRepository.existsByStoryIdAndViewerId(1L, 99L)).thenReturn(true);

        service.viewStory(1L, 99L);

        verify(storyViewRepository, never()).save(any());
        verify(storyRepository, never()).incrementViewsCount(any());
    }

    @Test
    void viewStory_byAuthor_shouldNotCountView() {
        Story story = buildStory(1L, 10L);
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(story));

        service.viewStory(1L, 10L); // author views own story

        verify(storyViewRepository, never()).existsByStoryIdAndViewerId(any(), any());
        verify(storyRepository, never()).incrementViewsCount(any());
    }

    @Test
    void viewStory_raceConditionDuplicate_shouldSwallowException() {
        Story story = buildStory(1L, 10L);
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(story));
        when(storyViewRepository.existsByStoryIdAndViewerId(1L, 99L)).thenReturn(false);
        when(storyViewRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

        // Must not throw
        assertThatNoException().isThrownBy(() -> service.viewStory(1L, 99L));
        verify(storyRepository, never()).incrementViewsCount(any());
    }

    @Test
    void viewStory_storyNotFound_shouldThrow() {
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.viewStory(1L, 99L))
                .isInstanceOf(StoryNotFoundException.class);
    }

    // ── getStoryViewers ───────────────────────────────────────────────────

    @Test
    void getStoryViewers_byAuthor_shouldReturnViewerList() {
        Story story = buildStory(1L, 10L);
        StoryView view = StoryView.builder()
                .id(1L).storyId(1L).viewerId(99L)
                .viewedAt(LocalDateTime.now()).build();
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(story));
        when(storyViewRepository.findByStoryIdOrderByViewedAtDesc(1L)).thenReturn(List.of(view));

        StoryViewersResponse response = service.getStoryViewers(1L, 10L);

        assertThat(response.getTotalViewers()).isEqualTo(1);
        assertThat(response.getViewers().get(0).getViewerId()).isEqualTo(99L);
    }

    @Test
    void getStoryViewers_byNonAuthor_shouldThrow() {
        Story story = buildStory(1L, 10L);
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(story));

        assertThatThrownBy(() -> service.getStoryViewers(1L, 99L))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("Only the story author");
    }

    @Test
    void getStoryViewers_storyNotFound_shouldThrow() {
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStoryViewers(1L, 10L))
                .isInstanceOf(StoryNotFoundException.class);
    }

    // ── deleteStory ───────────────────────────────────────────────────────

    @Test
    void deleteStory_byOwner_shouldDeactivate() {
        Story story = buildStory(1L, 10L);
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(story));

        service.deleteStory(1L, 10L);

        assertThat(story.getIsActive()).isFalse();
        verify(storyRepository).save(story);
    }

    @Test
    void deleteStory_byNonOwner_shouldThrow() {
        Story story = buildStory(1L, 10L);
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(story));

        assertThatThrownBy(() -> service.deleteStory(1L, 99L))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void deleteStory_notFound_shouldThrow() {
        when(storyRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteStory(1L, 10L))
                .isInstanceOf(StoryNotFoundException.class);
    }

    // ── expireOldStories ──────────────────────────────────────────────────

    @Test
    void expireOldStories_shouldReturnExpiredCount() {
        when(storyRepository.expireStories(any())).thenReturn(3);

        int count = service.expireOldStories();

        assertThat(count).isEqualTo(3);
        verify(storyRepository).expireStories(any(LocalDateTime.class));
    }

    @Test
    void expireOldStories_noneExpired_shouldReturnZero() {
        when(storyRepository.expireStories(any())).thenReturn(0);

        assertThat(service.expireOldStories()).isEqualTo(0);
    }

    @Test
    void createStory_rabbitTemplateThrows_shouldNotPropagateException() {
        StorageService.StorageResult result =
                new StorageService.StorageResult("http://localhost:8087/files/stories/s.jpg", "stories/s.jpg");
        when(storageService.store(any(), eq("stories"))).thenReturn(result);
        when(storyRepository.save(any())).thenReturn(buildStory(1L, 10L));
        when(followServiceClient.getFollowerIds(10L)).thenReturn(List.of(20L));
        doThrow(new RuntimeException("RabbitMQ down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // Story creation must succeed even if notification publishing fails
        assertThatNoException().isThrownBy(() -> service.createStory(10L, imageFile(), "caption"));
    }
}