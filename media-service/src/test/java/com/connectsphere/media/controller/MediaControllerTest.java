package com.connectsphere.media.controller;

import com.connectsphere.media.dto.response.MediaResponse;
import com.connectsphere.media.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class MediaControllerTest {

    private MockMvc mockMvc;
    private MediaService mediaService;
    private MediaResponse mediaResponse;

    @BeforeEach
    void setUp() {
        mediaService = mock(MediaService.class);
        mockMvc = standaloneSetup(new MediaController(mediaService)).build();

        mediaResponse = MediaResponse.builder()
                .id(1L)
                .uploaderId(10L)
                .url("http://localhost:8087/files/images/test.jpg")
                .mediaType("IMAGE")
                .sizeKb(10L)
                .mimeType("image/jpeg")
                .originalFilename("test.jpg")
                .linkedPostId(100L)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void uploadMedia_ShouldReturnCreatedMedia() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "image-data".getBytes());

        when(mediaService.uploadMedia(eq(10L), any(), eq(100L))).thenReturn(mediaResponse);

        mockMvc.perform(multipart("/api/v1/media")
                        .file(file)
                        .header("X-User-Id", 10L)
                        .param("linkedPostId", "100"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Media uploaded successfully"))
                .andExpect(jsonPath("$.data.mediaType").value("IMAGE"));
    }

    @Test
    void getMedia_ShouldReturnMedia() throws Exception {
        when(mediaService.getMediaById(1L)).thenReturn(mediaResponse);

        mockMvc.perform(get("/api/v1/media/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Media fetched"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getMediaByPost_ShouldReturnMediaList() throws Exception {
        when(mediaService.getMediaByPost(100L)).thenReturn(List.of(mediaResponse));

        mockMvc.perform(get("/api/v1/media/post/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Media for post"))
                .andExpect(jsonPath("$.data[0].linkedPostId").value(100));
    }

    @Test
    void getMediaByUploader_ShouldReturnPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<MediaResponse> page = new PageImpl<>(List.of(mediaResponse), pageable, 1);

        when(mediaService.getMediaByUploader(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/media/uploader/10")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Uploader media"))
                .andExpect(jsonPath("$.data.content[0].uploaderId").value(10));
    }

    @Test
    void deleteMedia_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/media/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Media deleted"));

        verify(mediaService).deleteMedia(1L, 10L);
    }

    @Test
    void softDeleteByPost_ShouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/v1/media/post/100/soft-delete"))
                .andExpect(status().isOk());

        verify(mediaService).softDeleteMediaByPost(100L);
    }
}