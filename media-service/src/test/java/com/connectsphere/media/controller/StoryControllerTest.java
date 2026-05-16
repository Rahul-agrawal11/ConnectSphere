package com.connectsphere.media.controller;

import com.connectsphere.media.dto.response.StoryResponse;
import com.connectsphere.media.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class StoryControllerTest {

    private MockMvc mockMvc;
    private MediaService mediaService;
    private StoryResponse ownerStoryResponse;
    private StoryResponse viewerStoryResponse;

    @BeforeEach
    void setUp() {
        mediaService = mock(MediaService.class);
        mockMvc = standaloneSetup(new StoryController(mediaService)).build();

        ownerStoryResponse = StoryResponse.builder()
                .id(1L)
                .authorId(10L)
                .mediaUrl("http://localhost:8087/files/stories/story.jpg")
                .caption("My story")
                .mediaType("IMAGE")
                .viewsCount(1)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .secondsUntilExpiry(86400L)
                .build();

        viewerStoryResponse = StoryResponse.builder()
                .id(1L)
                .authorId(10L)
                .mediaUrl("http://localhost:8087/files/stories/story.jpg")
                .caption("My story")
                .mediaType("IMAGE")
                .viewsCount(null)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .secondsUntilExpiry(86400L)
                .build();
    }

    @Test
    void createStory_ShouldReturnCreatedStory() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "story.jpg", "image/jpeg", "story-data".getBytes());

        when(mediaService.createStory(eq(10L), any(), eq("My story")))
                .thenReturn(ownerStoryResponse);

        mockMvc.perform(multipart("/api/v1/stories")
                        .file(file)
                        .header("X-User-Id", 10L)
                        .param("caption", "My story"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Story created"))
                .andExpect(jsonPath("$.data.caption").value("My story"))
                .andExpect(jsonPath("$.data.viewsCount").value(1));
    }

    @Test
    void getStory_AsOwner_ShouldReturnStoryWithViewsCount() throws Exception {
        when(mediaService.getStoryById(1L, 10L))
                .thenReturn(ownerStoryResponse);

        mockMvc.perform(get("/api/v1/stories/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Story fetched"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.viewsCount").value(1));

        verify(mediaService).getStoryById(1L, 10L);
    }

    @Test
    void getStory_AsViewer_ShouldReturnStoryWithoutViewsCount() throws Exception {
        when(mediaService.getStoryById(1L, 99L))
                .thenReturn(viewerStoryResponse);

        mockMvc.perform(get("/api/v1/stories/1")
                        .header("X-User-Id", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Story fetched"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.viewsCount").doesNotExist());

        verify(mediaService).getStoryById(1L, 99L);
    }

    @Test
    void getStoriesByUser_AsOwner_ShouldReturnStoriesWithViewsCount() throws Exception {
        when(mediaService.getActiveStoriesByUser(10L, 10L))
                .thenReturn(List.of(ownerStoryResponse));

        mockMvc.perform(get("/api/v1/stories/user/10")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User stories"))
                .andExpect(jsonPath("$.data[0].authorId").value(10))
                .andExpect(jsonPath("$.data[0].viewsCount").value(1));

        verify(mediaService).getActiveStoriesByUser(10L, 10L);
    }

    @Test
    void getStoriesByUser_AsViewer_ShouldReturnStoriesWithoutViewsCount() throws Exception {
        when(mediaService.getActiveStoriesByUser(10L, 99L))
                .thenReturn(List.of(viewerStoryResponse));

        mockMvc.perform(get("/api/v1/stories/user/10")
                        .header("X-User-Id", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User stories"))
                .andExpect(jsonPath("$.data[0].authorId").value(10))
                .andExpect(jsonPath("$.data[0].viewsCount").doesNotExist());

        verify(mediaService).getActiveStoriesByUser(10L, 99L);
    }

    @Test
    void getStoriesFeed_ShouldReturnStoriesWithoutViewsCountForViewer() throws Exception {
        when(mediaService.getStoriesFeed(List.of(10L, 20L), 99L))
                .thenReturn(List.of(viewerStoryResponse));

        mockMvc.perform(get("/api/v1/stories/feed")
                        .header("X-User-Id", 99L)
                        .param("followedUserIds", "10", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Stories feed"))
                .andExpect(jsonPath("$.data[0].mediaType").value("IMAGE"))
                .andExpect(jsonPath("$.data[0].viewsCount").doesNotExist());

        verify(mediaService).getStoriesFeed(List.of(10L, 20L), 99L);
    }

    @Test
    void viewStory_AsViewer_ShouldReturnStoryWithoutViewsCount() throws Exception {
        when(mediaService.viewStory(1L, 99L))
                .thenReturn(viewerStoryResponse);

        mockMvc.perform(post("/api/v1/stories/1/view")
                        .header("X-User-Id", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Story viewed"))
                .andExpect(jsonPath("$.data.viewsCount").doesNotExist());

        verify(mediaService).viewStory(1L, 99L);
    }

    @Test
    void viewStory_AsOwner_ShouldReturnStoryWithViewsCount() throws Exception {
        when(mediaService.viewStory(1L, 10L))
                .thenReturn(ownerStoryResponse);

        mockMvc.perform(post("/api/v1/stories/1/view")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Story viewed"))
                .andExpect(jsonPath("$.data.viewsCount").value(1));

        verify(mediaService).viewStory(1L, 10L);
    }

    @Test
    void deleteStory_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/stories/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Story deleted"));

        verify(mediaService).deleteStory(1L, 10L);
    }
}