package com.connectsphere.like.controller;

import com.connectsphere.like.dto.request.ReactRequest;
import com.connectsphere.like.dto.response.LikeResponse;
import com.connectsphere.like.dto.response.ReactionSummaryResponse;
import com.connectsphere.like.enums.ReactionType;
import com.connectsphere.like.enums.TargetType;
import com.connectsphere.like.service.LikeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class LikeControllerTest {

    private MockMvc mockMvc;
    private LikeService likeService;
    private ObjectMapper objectMapper;
    private LikeResponse likeResponse;

    @BeforeEach
    void setUp() {
        likeService = mock(LikeService.class);
        objectMapper = new ObjectMapper();
        mockMvc = standaloneSetup(new LikeController(likeService)).build();

        likeResponse = LikeResponse.builder()
                .id(1L)
                .userId(10L)
                .targetId(100L)
                .targetType("POST")
                .reactionType("LIKE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void react_ShouldReturnCreatedReaction() throws Exception {
        ReactRequest request = new ReactRequest();
        request.setTargetId(100L);
        request.setTargetType(TargetType.POST);
        request.setReactionType(ReactionType.LIKE);

        when(likeService.react(eq(10L), any(ReactRequest.class))).thenReturn(likeResponse);

        mockMvc.perform(post("/api/v1/likes")
                        .header("X-User-Id", 10L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reaction saved"))
                .andExpect(jsonPath("$.data.reactionType").value("LIKE"));
    }

    @Test
    void unreact_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/likes")
                        .header("X-User-Id", 10L)
                        .param("targetId", "100")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reaction removed"));

        verify(likeService).unreact(10L, 100L, TargetType.POST);
    }

    @Test
    void changeReaction_ShouldReturnUpdatedReaction() throws Exception {
        LikeResponse updated = LikeResponse.builder()
                .id(1L)
                .userId(10L)
                .targetId(100L)
                .targetType("POST")
                .reactionType("LOVE")
                .build();

        when(likeService.changeReaction(10L, 100L, TargetType.POST, ReactionType.LOVE))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/likes/change")
                        .header("X-User-Id", 10L)
                        .param("targetId", "100")
                        .param("targetType", "POST")
                        .param("newReactionType", "LOVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reaction updated"))
                .andExpect(jsonPath("$.data.reactionType").value("LOVE"));
    }

    @Test
    void hasReacted_ShouldReturnFalse_WhenUserIdMissing() throws Exception {
        mockMvc.perform(get("/api/v1/likes/has-reacted")
                        .param("targetId", "100")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Not reacted"))
                .andExpect(jsonPath("$.data").value(false));

        verify(likeService, never()).hasReacted(anyLong(), anyLong(), any(TargetType.class));
    }

    @Test
    void hasReacted_ShouldReturnTrue_WhenUserReacted() throws Exception {
        when(likeService.hasReacted(10L, 100L, TargetType.POST)).thenReturn(true);

        mockMvc.perform(get("/api/v1/likes/has-reacted")
                        .header("X-User-Id", 10L)
                        .param("targetId", "100")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Has reacted"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getUserReaction_ShouldReturnReaction() throws Exception {
        when(likeService.getUserReaction(10L, 100L, TargetType.POST)).thenReturn(likeResponse);

        mockMvc.perform(get("/api/v1/likes/my-reaction")
                        .header("X-User-Id", 10L)
                        .param("targetId", "100")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User reaction"))
                .andExpect(jsonPath("$.data.reactionType").value("LIKE"));
    }

    @Test
    void getReactionsByTarget_ShouldReturnPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<LikeResponse> page = new PageImpl<>(List.of(likeResponse), pageable, 1);

        when(likeService.getReactionsByTarget(eq(100L), eq(TargetType.POST), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/likes/target")
                        .param("targetId", "100")
                        .param("targetType", "POST")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reactions fetched"))
                .andExpect(jsonPath("$.data.content[0].reactionType").value("LIKE"));
    }

    @Test
    void getMyReactions_ShouldReturnPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<LikeResponse> page = new PageImpl<>(List.of(likeResponse), pageable, 1);

        when(likeService.getReactionsByUser(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/likes/my-reactions")
                        .header("X-User-Id", 10L)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("My reactions"))
                .andExpect(jsonPath("$.data.content[0].targetId").value(100));
    }

    @Test
    void getReactionCount_ShouldReturnCount() throws Exception {
        when(likeService.getReactionCount(100L, TargetType.POST)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/likes/count")
                        .param("targetId", "100")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reaction count"))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void getReactionCountByType_ShouldReturnCount() throws Exception {
        when(likeService.getReactionCountByType(100L, TargetType.POST, ReactionType.LOVE))
                .thenReturn(2L);

        mockMvc.perform(get("/api/v1/likes/count/by-type")
                        .param("targetId", "100")
                        .param("targetType", "POST")
                        .param("reactionType", "LOVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reaction count by type"))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    void getReactionSummary_ShouldReturnSummary() throws Exception {
        ReactionSummaryResponse summary = ReactionSummaryResponse.builder()
                .targetId(100L)
                .targetType("POST")
                .totalCount(5L)
                .reactions(Map.of("LIKE", 3L, "LOVE", 2L))
                .build();

        when(likeService.getReactionSummary(100L, TargetType.POST)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/likes/summary")
                        .param("targetId", "100")
                        .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reaction summary"))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.reactions.LIKE").value(3));
    }
}