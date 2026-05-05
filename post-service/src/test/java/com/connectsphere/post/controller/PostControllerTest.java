package com.connectsphere.post.controller;

import com.connectsphere.post.dto.request.CreatePostRequest;
import com.connectsphere.post.dto.request.UpdatePostRequest;
import com.connectsphere.post.dto.response.PostResponse;
import com.connectsphere.post.enums.PostVisibility;
import com.connectsphere.post.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PostControllerTest {

    private MockMvc mockMvc;
    private PostService postService;
    private ObjectMapper objectMapper;

    private PostResponse postResponse;

    @BeforeEach
    void setUp() {
        postService = mock(PostService.class);
        objectMapper = new ObjectMapper();

        mockMvc = standaloneSetup(new PostController(postService)).build();

        postResponse = PostResponse.builder()
                .id(1L)
                .authorId(10L)
                .content("Hello ConnectSphere")
                .mediaUrls(List.of())
                .postType("TEXT")
                .visibility("PUBLIC")
                .likesCount(0)
                .commentsCount(0)
                .sharesCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createPost_ShouldReturnCreatedPost() throws Exception {
        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Hello ConnectSphere");
        request.setVisibility(PostVisibility.PUBLIC);

        when(postService.createPost(eq(10L), any(CreatePostRequest.class))).thenReturn(postResponse);

        mockMvc.perform(post("/api/v1/posts")
                        .header("X-User-Id", 10L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post created"))
                .andExpect(jsonPath("$.data.content").value("Hello ConnectSphere"));
    }

    @Test
    void getPost_ShouldReturnPost() throws Exception {
        when(postService.getPostById(1L, 10L)).thenReturn(postResponse);

        mockMvc.perform(get("/api/v1/posts/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.authorId").value(10));
    }

    @Test
    void getPublicFeed_ShouldReturnPageOfPosts() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostResponse> page = new PageImpl<>(List.of(postResponse), pageable, 1);

        when(postService.getPublicFeed(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/posts/public")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value("Hello ConnectSphere"));
    }

    @Test
    void getPostsByUser_ShouldReturnUserPosts() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostResponse> page = new PageImpl<>(List.of(postResponse), pageable, 1);

        when(postService.getPostsByUser(eq(10L), eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/posts/user/10")
                        .header("X-User-Id", 10L)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].authorId").value(10));
    }

    @Test
    void getFeed_ShouldReturnPersonalizedFeed() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostResponse> page = new PageImpl<>(List.of(postResponse), pageable, 1);

        when(postService.getFeedForUser(eq(List.of(11L, 12L)), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/posts/feed")
                        .header("X-User-Id", 10L)
                        .param("followedUserIds", "11", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Feed fetched"));
    }

    @Test
    void searchPosts_ShouldReturnSearchResult() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostResponse> page = new PageImpl<>(List.of(postResponse), pageable, 1);

        when(postService.searchPosts(eq("hello"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/posts/search")
                        .param("keyword", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Search results"));
    }

    @Test
    void getPostCount_ShouldReturnCount() throws Exception {
        when(postService.getPostCount(10L)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/posts/count/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void updatePost_ShouldReturnUpdatedPost() throws Exception {
        UpdatePostRequest request = new UpdatePostRequest();
        request.setContent("Updated content");
        request.setVisibility(PostVisibility.PRIVATE);

        PostResponse updated = PostResponse.builder()
                .id(1L)
                .authorId(10L)
                .content("Updated content")
                .visibility("PRIVATE")
                .postType("TEXT")
                .likesCount(0)
                .commentsCount(0)
                .sharesCount(0)
                .build();

        when(postService.updatePost(eq(1L), eq(10L), any(UpdatePostRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/posts/1")
                        .header("X-User-Id", 10L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Updated content"))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"));
    }

    @Test
    void changeVisibility_ShouldReturnUpdatedVisibility() throws Exception {
        PostResponse updated = PostResponse.builder()
                .id(1L)
                .authorId(10L)
                .content("Hello ConnectSphere")
                .visibility("PRIVATE")
                .postType("TEXT")
                .likesCount(0)
                .commentsCount(0)
                .sharesCount(0)
                .build();

        when(postService.changeVisibility(1L, 10L, PostVisibility.PRIVATE)).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/posts/1/visibility")
                        .header("X-User-Id", 10L)
                        .param("visibility", "PRIVATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Visibility updated"))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"));
    }

    @Test
    void deletePost_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post deleted"));

        verify(postService).deletePost(1L, 10L);
    }

    @Test
    void adminDeletePost_ShouldReturnForbidden_WhenRoleIsNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/admin/1")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(postService, never()).adminDeletePost(anyLong());
    }

    @Test
    void adminDeletePost_ShouldDeletePost_WhenRoleIsAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/admin/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post removed by admin"));

        verify(postService).adminDeletePost(1L);
    }

    @Test
    void incrementLikes_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/posts/1/likes/increment"))
                .andExpect(status().isOk());

        verify(postService).incrementLikes(1L);
    }

    @Test
    void decrementLikes_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/posts/1/likes/decrement"))
                .andExpect(status().isOk());

        verify(postService).decrementLikes(1L);
    }

    @Test
    void incrementComments_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/posts/1/comments/increment"))
                .andExpect(status().isOk());

        verify(postService).incrementComments(1L);
    }

    @Test
    void incrementShares_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/posts/1/shares/increment"))
                .andExpect(status().isOk());

        verify(postService).incrementShares(1L);
    }
}