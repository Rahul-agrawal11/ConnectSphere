package com.connectsphere.follow.controller;

import com.connectsphere.follow.dto.response.FollowCountResponse;
import com.connectsphere.follow.dto.response.FollowResponse;
import com.connectsphere.follow.service.FollowService;
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

class FollowControllerTest {

    private MockMvc mockMvc;
    private FollowService followService;
    private FollowResponse followResponse;

    @BeforeEach
    void setUp() {
        followService = mock(FollowService.class);
        mockMvc = standaloneSetup(new FollowController(followService)).build();

        followResponse = FollowResponse.builder()
                .id(1L)
                .followerId(10L)
                .followeeId(20L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void follow_ShouldReturnCreatedFollow() throws Exception {
        when(followService.follow(10L, 20L)).thenReturn(followResponse);

        mockMvc.perform(post("/api/v1/follows/20")
                        .header("X-User-Id", 10L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Successfully followed user"))
                .andExpect(jsonPath("$.data.followerId").value(10))
                .andExpect(jsonPath("$.data.followeeId").value(20));
    }

    @Test
    void unfollow_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/follows/20")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully unfollowed user"));

        verify(followService).unfollow(10L, 20L);
    }

    @Test
    void isFollowing_ShouldReturnFalse_WhenUserIdMissing() throws Exception {
        mockMvc.perform(get("/api/v1/follows/is-following/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Not following"))
                .andExpect(jsonPath("$.data").value(false));

        verify(followService, never()).isFollowing(anyLong(), anyLong());
    }

    @Test
    void isFollowing_ShouldReturnTrue_WhenFollowing() throws Exception {
        when(followService.isFollowing(10L, 20L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/follows/is-following/20")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Is following"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getFollowers_ShouldReturnFollowersPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<FollowResponse> page = new PageImpl<>(List.of(followResponse), pageable, 1);

        when(followService.getFollowers(eq(20L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/follows/20/followers")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Followers fetched"))
                .andExpect(jsonPath("$.data.content[0].followerId").value(10));
    }

    @Test
    void getFollowing_ShouldReturnFollowingPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<FollowResponse> page = new PageImpl<>(List.of(followResponse), pageable, 1);

        when(followService.getFollowing(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/follows/10/following")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Following fetched"))
                .andExpect(jsonPath("$.data.content[0].followeeId").value(20));
    }

    @Test
    void getFollowCounts_ShouldReturnCounts() throws Exception {
        FollowCountResponse counts = FollowCountResponse.builder()
                .userId(10L)
                .followerCount(5L)
                .followingCount(3L)
                .build();

        when(followService.getFollowCounts(10L)).thenReturn(counts);

        mockMvc.perform(get("/api/v1/follows/10/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follow counts"))
                .andExpect(jsonPath("$.data.followerCount").value(5))
                .andExpect(jsonPath("$.data.followingCount").value(3));
    }

    @Test
    void getFollowerCount_ShouldReturnCount() throws Exception {
        when(followService.getFollowerCount(10L)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/follows/10/followers/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Follower count"))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void getFollowingCount_ShouldReturnCount() throws Exception {
        when(followService.getFollowingCount(10L)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/follows/10/following/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Following count"))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void getMutualFollowIds_ShouldReturnIds() throws Exception {
        when(followService.getMutualFollowIds(10L)).thenReturn(List.of(20L, 30L));

        mockMvc.perform(get("/api/v1/follows/10/mutual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mutual follow IDs"))
                .andExpect(jsonPath("$.data[0]").value(20))
                .andExpect(jsonPath("$.data[1]").value(30));
    }

    @Test
    void getSuggestedUsers_ShouldReturnIds() throws Exception {
        when(followService.getSuggestedUserIds(10L, 10)).thenReturn(List.of(40L, 50L));

        mockMvc.perform(get("/api/v1/follows/suggestions")
                        .header("X-User-Id", 10L)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Suggested users"))
                .andExpect(jsonPath("$.data[0]").value(40))
                .andExpect(jsonPath("$.data[1]").value(50));
    }

    @Test
    void getFollowingIds_ShouldReturnPlainList() throws Exception {
        when(followService.getFollowingIds(10L)).thenReturn(List.of(20L, 30L));

        mockMvc.perform(get("/api/v1/follows/internal/following-ids/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(20))
                .andExpect(jsonPath("$[1]").value(30));
    }

    @Test
    void getFollowerIds_ShouldReturnPlainList() throws Exception {
        when(followService.getFollowerIds(20L)).thenReturn(List.of(10L, 30L));

        mockMvc.perform(get("/api/v1/follows/internal/follower-ids/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(10))
                .andExpect(jsonPath("$[1]").value(30));
    }
}