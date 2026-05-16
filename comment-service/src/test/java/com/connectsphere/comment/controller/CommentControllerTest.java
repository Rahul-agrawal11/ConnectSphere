package com.connectsphere.comment.controller;

import com.connectsphere.comment.dto.request.AddCommentRequest;
import com.connectsphere.comment.dto.request.UpdateCommentRequest;
import com.connectsphere.comment.dto.response.CommentResponse;
import com.connectsphere.comment.service.CommentService;
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

class CommentControllerTest {

    private MockMvc mockMvc;
    private CommentService commentService;
    private ObjectMapper objectMapper;
    private CommentResponse commentResponse;

    @BeforeEach
    void setUp() {
        commentService = mock(CommentService.class);
        objectMapper = new ObjectMapper();
        mockMvc = standaloneSetup(new CommentController(commentService)).build();

        commentResponse = CommentResponse.builder()
                .id(1L)
                .postId(100L)
                .authorId(10L)
                .parentCommentId(null)
                .content("Nice post")
                .likesCount(0)
                .isReply(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void addComment_ShouldReturnCreatedComment() throws Exception {
        AddCommentRequest request = new AddCommentRequest();
        request.setPostId(100L);
        request.setContent("Nice post");

        when(commentService.addComment(eq(10L), any(AddCommentRequest.class))).thenReturn(commentResponse);

        mockMvc.perform(post("/api/v1/comments")
                        .header("X-User-Id", 10L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Comment added"))
                .andExpect(jsonPath("$.data.content").value("Nice post"));
    }

    @Test
    void getComment_ShouldReturnComment() throws Exception {
        when(commentService.getCommentById(1L)).thenReturn(commentResponse);

        mockMvc.perform(get("/api/v1/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment fetched"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getCommentsByPost_ShouldReturnComments() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CommentResponse> page = new PageImpl<>(List.of(commentResponse), pageable, 1);

        when(commentService.getCommentsByPost(eq(100L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/comments/post/100")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comments fetched"))
                .andExpect(jsonPath("$.data.content[0].content").value("Nice post"));
    }

    @Test
    void getReplies_ShouldReturnReplies() throws Exception {
        CommentResponse reply = CommentResponse.builder()
                .id(2L)
                .postId(100L)
                .authorId(20L)
                .parentCommentId(1L)
                .content("Reply")
                .likesCount(0)
                .isReply(true)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<CommentResponse> page = new PageImpl<>(List.of(reply), pageable, 1);

        when(commentService.getReplies(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/comments/1/replies")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Replies fetched"))
                .andExpect(jsonPath("$.data.content[0].isReply").value(true));
    }

    @Test
    void getCommentsByUser_ShouldReturnUserComments() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CommentResponse> page = new PageImpl<>(List.of(commentResponse), pageable, 1);

        when(commentService.getCommentsByUser(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/comments/user/10")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User comments fetched"))
                .andExpect(jsonPath("$.data.content[0].authorId").value(10));
    }

    @Test
    void getCommentCount_ShouldReturnCount() throws Exception {
        when(commentService.getCommentCount(100L)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/comments/post/100/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment count"))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void getTotalCommentCount_ShouldReturnTotalCount() throws Exception {
        when(commentService.getTotalCommentCount(100L)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/comments/post/100/count/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Total comment count"))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void updateComment_ShouldReturnUpdatedComment() throws Exception {
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated comment");

        CommentResponse updated = CommentResponse.builder()
                .id(1L)
                .postId(100L)
                .authorId(10L)
                .content("Updated comment")
                .likesCount(0)
                .isReply(false)
                .build();

        when(commentService.updateComment(eq(1L), eq(10L), any(UpdateCommentRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/comments/1")
                        .header("X-User-Id", 10L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment updated"))
                .andExpect(jsonPath("$.data.content").value("Updated comment"));
    }

    @Test
    void deleteComment_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment deleted"));

        verify(commentService).deleteComment(1L, 10L);
    }

    @Test
    void adminDeleteComment_ShouldReturnForbidden_WhenRoleIsNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/admin/1")
                        .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(commentService, never()).adminDeleteComment(anyLong());
    }

    @Test
    void adminDeleteComment_ShouldDelete_WhenRoleIsAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/admin/1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment removed by admin"));

        verify(commentService).adminDeleteComment(1L);
    }

    @Test
    void likeComment_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/comments/1/likes/increment"))
                .andExpect(status().isOk());

        verify(commentService).likeComment(1L);
    }

    @Test
    void unlikeComment_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/comments/1/likes/decrement"))
                .andExpect(status().isOk());

        verify(commentService).unlikeComment(1L);
    }
}