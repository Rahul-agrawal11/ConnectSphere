package com.connectsphere.comment.service;

import com.connectsphere.comment.client.PostServiceClient;
import com.connectsphere.comment.dto.request.AddCommentRequest;
import com.connectsphere.comment.dto.request.UpdateCommentRequest;
import com.connectsphere.comment.dto.response.CommentResponse;
import com.connectsphere.comment.entity.Comment;
import com.connectsphere.comment.exception.CommentNotFoundException;
import com.connectsphere.comment.exception.UnauthorizedActionException;
import com.connectsphere.comment.repository.CommentRepository;
import com.connectsphere.comment.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostServiceClient postServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment comment;

    @BeforeEach
    void setUp() {
        comment = Comment.builder()
                .id(1L)
                .postId(100L)
                .authorId(10L)
                .parentCommentId(null)
                .content("Nice post")
                .likesCount(0)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void addComment_ShouldCreateTopLevelComment() {
        AddCommentRequest request = new AddCommentRequest();
        request.setPostId(100L);
        request.setContent("Nice post");

        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentResponse response = commentService.addComment(10L, request);

        assertNotNull(response);
        assertEquals(100L, response.getPostId());
        assertEquals(10L, response.getAuthorId());
        assertEquals("Nice post", response.getContent());
        assertFalse(response.getIsReply());

        verify(commentRepository).save(any(Comment.class));
        verify(postServiceClient).incrementCommentsCount(100L);
    }

    @Test
    void addComment_ShouldCreateReply_WhenParentCommentIsTopLevel() {
        AddCommentRequest request = new AddCommentRequest();
        request.setPostId(100L);
        request.setParentCommentId(1L);
        request.setContent("Reply comment");

        Comment reply = Comment.builder()
                .id(2L)
                .postId(100L)
                .authorId(20L)
                .parentCommentId(1L)
                .content("Reply comment")
                .likesCount(0)
                .isDeleted(false)
                .build();

        when(commentRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(reply);

        CommentResponse response = commentService.addComment(20L, request);

        assertTrue(response.getIsReply());
        assertEquals(1L, response.getParentCommentId());
        verify(postServiceClient).incrementCommentsCount(100L);
    }

    @Test
    void addComment_ShouldThrowException_WhenReplyingToReply() {
        AddCommentRequest request = new AddCommentRequest();
        request.setPostId(100L);
        request.setParentCommentId(2L);
        request.setContent("Invalid reply");

        Comment replyParent = Comment.builder()
                .id(2L)
                .postId(100L)
                .authorId(20L)
                .parentCommentId(1L)
                .content("Existing reply")
                .isDeleted(false)
                .build();

        when(commentRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(replyParent));

        assertThrows(IllegalArgumentException.class, () -> commentService.addComment(30L, request));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void getCommentById_ShouldReturnComment() {
        when(commentRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));

        CommentResponse response = commentService.getCommentById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Nice post", response.getContent());
    }

    @Test
    void getCommentById_ShouldThrowException_WhenNotFound() {
        when(commentRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class, () -> commentService.getCommentById(1L));
    }

    @Test
    void updateComment_ShouldUpdateComment_WhenOwner() {
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated comment");

        when(commentRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.updateComment(1L, 10L, request);

        assertEquals("Updated comment", response.getContent());
        verify(commentRepository).save(comment);
    }

    @Test
    void updateComment_ShouldThrowException_WhenNotOwner() {
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated comment");

        when(commentRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));

        assertThrows(UnauthorizedActionException.class,
                () -> commentService.updateComment(1L, 99L, request));

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void deleteComment_ShouldSoftDeleteComment_WhenOwner() {
        when(commentRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(1L, 10L);

        assertTrue(comment.getIsDeleted());
        assertEquals("[deleted]", comment.getContent());
        verify(commentRepository).save(comment);
        verify(postServiceClient).decrementCommentsCount(100L);
    }

    @Test
    void deleteComment_ShouldThrowException_WhenNotOwner() {
        when(commentRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));

        assertThrows(UnauthorizedActionException.class,
                () -> commentService.deleteComment(1L, 99L));

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void getCommentsByPost_ShouldReturnComments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> page = new PageImpl<>(List.of(comment), pageable, 1);

        when(commentRepository.findTopLevelByPostId(100L, pageable)).thenReturn(page);

        Page<CommentResponse> response = commentService.getCommentsByPost(100L, pageable);

        assertEquals(1, response.getContent().size());
    }

    @Test
    void getReplies_ShouldReturnReplies() {
        Pageable pageable = PageRequest.of(0, 10);

        Comment reply = Comment.builder()
                .id(2L)
                .postId(100L)
                .authorId(20L)
                .parentCommentId(1L)
                .content("Reply")
                .likesCount(0)
                .isDeleted(false)
                .build();

        Page<Comment> page = new PageImpl<>(List.of(reply), pageable, 1);

        when(commentRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.findRepliesByParentCommentId(1L, pageable)).thenReturn(page);

        Page<CommentResponse> response = commentService.getReplies(1L, pageable);

        assertEquals(1, response.getContent().size());
        assertTrue(response.getContent().get(0).getIsReply());
    }

    @Test
    void getCommentsByUser_ShouldReturnUserComments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> page = new PageImpl<>(List.of(comment), pageable, 1);

        when(commentRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(10L, pageable))
                .thenReturn(page);

        Page<CommentResponse> response = commentService.getCommentsByUser(10L, pageable);

        assertEquals(1, response.getContent().size());
    }

    @Test
    void likeComment_ShouldIncrementLikes() {
        when(commentRepository.incrementLikesCount(1L)).thenReturn(1);

        commentService.likeComment(1L);

        verify(commentRepository).incrementLikesCount(1L);
    }

    @Test
    void likeComment_ShouldThrowException_WhenCommentNotFound() {
        when(commentRepository.incrementLikesCount(1L)).thenReturn(0);

        assertThrows(CommentNotFoundException.class, () -> commentService.likeComment(1L));
    }

    @Test
    void unlikeComment_ShouldDecrementLikes() {
        when(commentRepository.decrementLikesCount(1L)).thenReturn(1);

        commentService.unlikeComment(1L);

        verify(commentRepository).decrementLikesCount(1L);
    }

    @Test
    void getCommentCount_ShouldReturnTopLevelCount() {
        when(commentRepository.countTopLevelByPostId(100L)).thenReturn(3L);

        long count = commentService.getCommentCount(100L);

        assertEquals(3L, count);
    }

    @Test
    void getTotalCommentCount_ShouldReturnTotalCount() {
        when(commentRepository.countAllByPostId(100L)).thenReturn(5L);

        long count = commentService.getTotalCommentCount(100L);

        assertEquals(5L, count);
    }

    @Test
    void adminDeleteComment_ShouldSoftDeleteAnyComment() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.adminDeleteComment(1L);

        assertTrue(comment.getIsDeleted());
        assertEquals("[deleted]", comment.getContent());
        verify(commentRepository).save(comment);
    }
}