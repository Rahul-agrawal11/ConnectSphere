package com.connectsphere.search.controller;

import com.connectsphere.search.dto.response.HashtagResponse;
import com.connectsphere.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SearchControllerTest {

    private MockMvc mockMvc;
    private SearchService searchService;
    private HashtagResponse hashtagResponse;

    @BeforeEach
    void setUp() {
        searchService = mock(SearchService.class);
        mockMvc = standaloneSetup(new SearchController(searchService)).build();

        hashtagResponse = HashtagResponse.builder()
                .id(1L)
                .tag("springboot")
                .postCount(5)
                .lastUsedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void searchPosts_ShouldReturnPostIdsPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Long> page = new PageImpl<>(List.of(100L, 101L), pageable, 2);

        when(searchService.searchPostIds(eq("java"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/search/posts")
                        .param("keyword", "java")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post search results"))
                .andExpect(jsonPath("$.data.content[0]").value(100));
    }

    @Test
    void searchUsers_ShouldReturnUsers() throws Exception {
        Object users = List.of(Map.of("username", "rahul"));

        when(searchService.searchUsers("rahul")).thenReturn(users);

        mockMvc.perform(get("/api/v1/search/users")
                        .param("query", "rahul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User search results"))
                .andExpect(jsonPath("$.data[0].username").value("rahul"));
    }

    @Test
    void indexPost_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/hashtags/index")
                        .param("postId", "100")
                        .param("content", "Hello #java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post indexed successfully"));

        verify(searchService).indexPost(100L, "Hello #java");
    }

    @Test
    void removePostIndex_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/hashtags/index/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post index removed"));

        verify(searchService).removePostIndex(100L);
    }

    @Test
    void getHashtag_ShouldReturnHashtag() throws Exception {
        when(searchService.getHashtagByTag("springboot")).thenReturn(hashtagResponse);

        mockMvc.perform(get("/api/v1/hashtags/springboot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hashtag found"))
                .andExpect(jsonPath("$.data.tag").value("springboot"));
    }

    @Test
    void getHashtagsForPost_ShouldReturnHashtags() throws Exception {
        when(searchService.getHashtagsForPost(100L)).thenReturn(List.of(hashtagResponse));

        mockMvc.perform(get("/api/v1/hashtags/post/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hashtags for post"))
                .andExpect(jsonPath("$.data[0].tag").value("springboot"));
    }

    @Test
    void getTrending_ShouldReturnTrendingHashtags() throws Exception {
        when(searchService.getTrendingHashtags(10)).thenReturn(List.of(hashtagResponse));

        mockMvc.perform(get("/api/v1/hashtags/trending")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Trending hashtags"))
                .andExpect(jsonPath("$.data[0].postCount").value(5));
    }

    @Test
    void getPostsByHashtag_ShouldReturnPostIds() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Long> page = new PageImpl<>(List.of(100L, 101L), pageable, 2);

        when(searchService.getPostIdsByHashtag(eq("springboot"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/hashtags/springboot/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Posts for hashtag"))
                .andExpect(jsonPath("$.data.content[0]").value(100));
    }

    @Test
    void searchHashtags_ShouldReturnMatchingHashtags() throws Exception {
        when(searchService.searchHashtags("spring", 10)).thenReturn(List.of(hashtagResponse));

        mockMvc.perform(get("/api/v1/search/hashtags")
                        .param("query", "spring")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hashtag search results"))
                .andExpect(jsonPath("$.data[0].tag").value("springboot"));
    }

    @Test
    void getHashtagCount_ShouldReturnCount() throws Exception {
        when(searchService.getPostCountByHashtag("springboot")).thenReturn(7L);

        mockMvc.perform(get("/api/v1/hashtags/springboot/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hashtag post count"))
                .andExpect(jsonPath("$.data").value(7));
    }
}