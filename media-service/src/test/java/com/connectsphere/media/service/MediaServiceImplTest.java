package com.connectsphere.media.service;

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
import com.connectsphere.media.service.impl.MediaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private Media media;
    private Story story;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaService, "storyExpiryHours", 24);

        media = Media.builder()
                .id(1L)
                .uploaderId(10L)
                .url("http://localhost:8087/files/images/test.jpg")
                .storageKey("images/test.jpg")
                .mediaType(MediaType.IMAGE)
                .sizeKb(10L)
                .mimeType("image/jpeg")
                .originalFilename("test.jpg")
                .linkedPostId(100L)
                .isDeleted(false)
                .uploadedAt(LocalDateTime.now())
                .build();

        story = Story.builder()
                .id(1L)
                .authorId(10L)
                .mediaUrl("http://localhost:8087/files/stories/story.jpg")
                .caption("My story")
                .mediaType(MediaType.IMAGE)
                .viewsCount(0)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void uploadMedia_ShouldUploadImageSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "image-data".getBytes());

        when(storageService.store(eq(file), eq("images")))
                .thenReturn(new StorageService.StorageResult(
                        "http://localhost:8087/files/images/test.jpg",
                        "images/test.jpg"
                ));

        when(mediaRepository.save(any(Media.class))).thenReturn(media);

        MediaResponse response = mediaService.uploadMedia(10L, file, 100L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getUploaderId());
        assertEquals("IMAGE", response.getMediaType());
        assertEquals(100L, response.getLinkedPostId());

        verify(storageService).store(file, "images");
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void uploadMedia_ShouldUploadVideoSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "video-data".getBytes());

        Media video = Media.builder()
                .id(2L)
                .uploaderId(10L)
                .url("http://localhost:8087/files/videos/video.mp4")
                .storageKey("videos/video.mp4")
                .mediaType(MediaType.VIDEO)
                .sizeKb(10L)
                .mimeType("video/mp4")
                .originalFilename("video.mp4")
                .isDeleted(false)
                .build();

        when(storageService.store(eq(file), eq("videos")))
                .thenReturn(new StorageService.StorageResult(
                        "http://localhost:8087/files/videos/video.mp4",
                        "videos/video.mp4"
                ));

        when(mediaRepository.save(any(Media.class))).thenReturn(video);

        MediaResponse response = mediaService.uploadMedia(10L, file, null);

        assertEquals("VIDEO", response.getMediaType());
        verify(storageService).store(file, "videos");
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    void uploadMedia_ShouldThrowException_WhenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> mediaService.uploadMedia(10L, file, null));

        verify(storageService, never()).store(any(), anyString());
    }

    @Test
    void uploadMedia_ShouldThrowException_WhenFileTypeNotAllowed() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "file.pdf", "application/pdf", "pdf".getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> mediaService.uploadMedia(10L, file, null));

        verify(storageService, never()).store(any(), anyString());
    }

    @Test
    void getMediaById_ShouldReturnMedia() {
        when(mediaRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(media));

        MediaResponse response = mediaService.getMediaById(1L);

        assertEquals(1L, response.getId());
        assertEquals("IMAGE", response.getMediaType());

        verify(mediaRepository).findByIdAndIsDeletedFalse(1L);
    }

    @Test
    void getMediaById_ShouldThrowException_WhenNotFound() {
        when(mediaRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.empty());

        assertThrows(MediaNotFoundException.class,
                () -> mediaService.getMediaById(1L));
    }

    @Test
    void getMediaByPost_ShouldReturnMediaList() {
        when(mediaRepository.findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc(100L))
                .thenReturn(List.of(media));

        List<MediaResponse> response = mediaService.getMediaByPost(100L);

        assertEquals(1, response.size());
        assertEquals(100L, response.get(0).getLinkedPostId());
    }

    @Test
    void getMediaByUploader_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Media> page = new PageImpl<>(List.of(media), pageable, 1);

        when(mediaRepository.findByUploaderIdAndIsDeletedFalseOrderByUploadedAtDesc(10L, pageable))
                .thenReturn(page);

        Page<MediaResponse> response = mediaService.getMediaByUploader(10L, pageable);

        assertEquals(1, response.getContent().size());
    }

    @Test
    void deleteMedia_ShouldSoftDelete_WhenOwner() {
        when(mediaRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(media));

        mediaService.deleteMedia(1L, 10L);

        assertTrue(media.getIsDeleted());
        verify(mediaRepository).save(media);
    }

    @Test
    void deleteMedia_ShouldThrowException_WhenNotOwner() {
        when(mediaRepository.findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(media));

        assertThrows(UnauthorizedActionException.class,
                () -> mediaService.deleteMedia(1L, 99L));

        verify(mediaRepository, never()).save(any(Media.class));
    }

    @Test
    void softDeleteMediaByPost_ShouldSoftDeleteAllPostMedia() {
        when(mediaRepository.findByLinkedPostIdAndIsDeletedFalseOrderByUploadedAtAsc(100L))
                .thenReturn(List.of(media));

        mediaService.softDeleteMediaByPost(100L);

        assertTrue(media.getIsDeleted());
        verify(mediaRepository).saveAll(List.of(media));
    }

    @Test
    void createStory_ShouldCreateStorySuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "story.jpg", "image/jpeg", "story-data".getBytes());

        when(storageService.store(eq(file), eq("stories")))
                .thenReturn(new StorageService.StorageResult(
                        "http://localhost:8087/files/stories/story.jpg",
                        "stories/story.jpg"
                ));

        when(storyRepository.save(any(Story.class))).thenReturn(story);

        StoryResponse response = mediaService.createStory(10L, file, "My story");

        assertNotNull(response);
        assertEquals(10L, response.getAuthorId());
        assertEquals("My story", response.getCaption());
        assertEquals("IMAGE", response.getMediaType());
        assertEquals(0, response.getViewsCount());

        verify(storageService).store(file, "stories");
        verify(storyRepository).save(any(Story.class));
    }

    @Test
    void getStoryById_AsOwner_ShouldReturnActiveStoryWithViewsCount() {
        when(storyRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(story));

        StoryResponse response = mediaService.getStoryById(1L, 10L);

        assertEquals(1L, response.getId());
        assertEquals(10L, response.getAuthorId());
        assertEquals(0, response.getViewsCount());
    }

    @Test
    void getStoryById_AsViewer_ShouldReturnActiveStoryWithoutViewsCount() {
        when(storyRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(story));

        StoryResponse response = mediaService.getStoryById(1L, 99L);

        assertEquals(1L, response.getId());
        assertEquals(10L, response.getAuthorId());
        assertNull(response.getViewsCount());
    }

    @Test
    void getStoryById_ShouldThrowException_WhenNotFound() {
        when(storyRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.empty());

        assertThrows(StoryNotFoundException.class,
                () -> mediaService.getStoryById(1L, 10L));
    }

    @Test
    void getActiveStoriesByUser_AsOwner_ShouldReturnStoriesWithViewsCount() {
        when(storyRepository.findByAuthorIdAndIsActiveTrueOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(story));

        List<StoryResponse> response = mediaService.getActiveStoriesByUser(10L, 10L);

        assertEquals(1, response.size());
        assertEquals(0, response.get(0).getViewsCount());
    }

    @Test
    void getActiveStoriesByUser_AsViewer_ShouldReturnStoriesWithoutViewsCount() {
        when(storyRepository.findByAuthorIdAndIsActiveTrueOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(story));

        List<StoryResponse> response = mediaService.getActiveStoriesByUser(10L, 99L);

        assertEquals(1, response.size());
        assertNull(response.get(0).getViewsCount());
    }

    @Test
    void getStoriesFeed_ShouldReturnEmptyList_WhenFollowedUserIdsEmpty() {
        List<StoryResponse> response = mediaService.getStoriesFeed(List.of(), 99L);

        assertTrue(response.isEmpty());
        verify(storyRepository, never()).findActiveStoriesByAuthorIds(anyList());
    }

    @Test
    void getStoriesFeed_ShouldReturnStoriesWithoutViewsCountForViewer() {
        when(storyRepository.findActiveStoriesByAuthorIds(List.of(10L, 20L)))
                .thenReturn(List.of(story));

        List<StoryResponse> response = mediaService.getStoriesFeed(List.of(10L, 20L), 99L);

        assertEquals(1, response.size());
        assertNull(response.get(0).getViewsCount());
    }

    @Test
    void getStoriesFeed_ShouldReturnStoriesWithViewsCountForOwnerStory() {
        when(storyRepository.findActiveStoriesByAuthorIds(List.of(10L)))
                .thenReturn(List.of(story));

        List<StoryResponse> response = mediaService.getStoriesFeed(List.of(10L), 10L);

        assertEquals(1, response.size());
        assertEquals(0, response.get(0).getViewsCount());
    }

    @Test
    void viewStory_ShouldIncrementViews_WhenViewerIsNotAuthor_AndHideViewsCount() {
        when(storyRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(story));

        when(storyRepository.incrementViewsCount(1L))
                .thenReturn(1);

        StoryResponse response = mediaService.viewStory(1L, 99L);

        assertNull(response.getViewsCount());
        verify(storyRepository).incrementViewsCount(1L);
    }

    @Test
    void viewStory_ShouldNotIncrementViews_WhenViewerIsAuthor_AndShowViewsCount() {
        when(storyRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(story));

        StoryResponse response = mediaService.viewStory(1L, 10L);

        assertEquals(0, response.getViewsCount());
        verify(storyRepository, never()).incrementViewsCount(anyLong());
    }

    @Test
    void deleteStory_ShouldSoftDelete_WhenAuthor() {
        when(storyRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(story));

        mediaService.deleteStory(1L, 10L);

        assertFalse(story.getIsActive());
        verify(storyRepository).save(story);
    }

    @Test
    void deleteStory_ShouldThrowException_WhenNotAuthor() {
        when(storyRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(story));

        assertThrows(UnauthorizedActionException.class,
                () -> mediaService.deleteStory(1L, 99L));

        verify(storyRepository, never()).save(any(Story.class));
    }

    @Test
    void expireOldStories_ShouldReturnExpiredCount() {
        when(storyRepository.expireStories(any(LocalDateTime.class)))
                .thenReturn(3);

        int result = mediaService.expireOldStories();

        assertEquals(3, result);
    }
}