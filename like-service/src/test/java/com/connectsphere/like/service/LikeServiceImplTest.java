package com.connectsphere.like.service;

import com.connectsphere.like.client.CommentServiceClient;
import com.connectsphere.like.client.PostServiceClient;
import com.connectsphere.like.dto.request.ReactRequest;
import com.connectsphere.like.dto.response.LikeResponse;
import com.connectsphere.like.dto.response.ReactionSummaryResponse;
import com.connectsphere.like.entity.Like;
import com.connectsphere.like.enums.ReactionType;
import com.connectsphere.like.enums.TargetType;
import com.connectsphere.like.exception.DuplicateReactionException;
import com.connectsphere.like.exception.LikeNotFoundException;
import com.connectsphere.like.repository.LikeRepository;
import com.connectsphere.like.service.impl.LikeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private PostServiceClient postServiceClient;

    @Mock
    private CommentServiceClient commentServiceClient;

    @InjectMocks
    private LikeServiceImpl likeService;

    private Like like;

    @BeforeEach
    void setUp() {
        like = Like.builder()
                .id(1L)
                .userId(10L)
                .targetId(100L)
                .targetType(TargetType.POST)
                .reactionType(ReactionType.LIKE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void react_ShouldSaveReactionForPost() {
        ReactRequest request = new ReactRequest();
        request.setTargetId(100L);
        request.setTargetType(TargetType.POST);
        request.setReactionType(ReactionType.LIKE);

        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST))
                .thenReturn(false);
        when(likeRepository.save(any(Like.class))).thenReturn(like);

        LikeResponse response = likeService.react(10L, request);

        assertNotNull(response);
        assertEquals(10L, response.getUserId());
        assertEquals(100L, response.getTargetId());
        assertEquals("POST", response.getTargetType());
        assertEquals("LIKE", response.getReactionType());

        verify(likeRepository).save(any(Like.class));
        verify(postServiceClient).incrementLikesCount(100L);
        verify(commentServiceClient, never()).incrementLikesCount(anyLong());
    }

    @Test
    void react_ShouldSaveReactionForComment() {
        ReactRequest request = new ReactRequest();
        request.setTargetId(200L);
        request.setTargetType(TargetType.COMMENT);
        request.setReactionType(ReactionType.LOVE);

        Like commentLike = Like.builder()
                .id(2L)
                .userId(10L)
                .targetId(200L)
                .targetType(TargetType.COMMENT)
                .reactionType(ReactionType.LOVE)
                .build();

        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(10L, 200L, TargetType.COMMENT))
                .thenReturn(false);
        when(likeRepository.save(any(Like.class))).thenReturn(commentLike);

        LikeResponse response = likeService.react(10L, request);

        assertEquals("COMMENT", response.getTargetType());
        assertEquals("LOVE", response.getReactionType());

        verify(commentServiceClient).incrementLikesCount(200L);
        verify(postServiceClient, never()).incrementLikesCount(anyLong());
    }

    @Test
    void react_ShouldThrowException_WhenReactionAlreadyExists() {
        ReactRequest request = new ReactRequest();
        request.setTargetId(100L);
        request.setTargetType(TargetType.POST);
        request.setReactionType(ReactionType.LIKE);

        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST))
                .thenReturn(true);

        assertThrows(DuplicateReactionException.class, () -> likeService.react(10L, request));

        verify(likeRepository, never()).save(any(Like.class));
    }

    @Test
    void unreact_ShouldDeleteReactionAndDecrementPostCounter() {
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST))
                .thenReturn(Optional.of(like));

        likeService.unreact(10L, 100L, TargetType.POST);

        verify(likeRepository).deleteByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST);
        verify(postServiceClient).decrementLikesCount(100L);
    }

    @Test
    void unreact_ShouldThrowException_WhenReactionNotFound() {
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST))
                .thenReturn(Optional.empty());

        assertThrows(LikeNotFoundException.class,
                () -> likeService.unreact(10L, 100L, TargetType.POST));
    }

    @Test
    void changeReaction_ShouldUpdateReactionType() {
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST))
                .thenReturn(Optional.of(like));
        when(likeRepository.save(any(Like.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LikeResponse response = likeService.changeReaction(10L, 100L, TargetType.POST, ReactionType.LOVE);

        assertEquals("LOVE", response.getReactionType());
        verify(likeRepository).save(like);
    }

    @Test
    void changeReaction_ShouldReturnSameReaction_WhenReactionTypeSame() {
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST))
                .thenReturn(Optional.of(like));

        LikeResponse response = likeService.changeReaction(10L, 100L, TargetType.POST, ReactionType.LIKE);

        assertEquals("LIKE", response.getReactionType());
        verify(likeRepository, never()).save(any(Like.class));
    }

    @Test
    void changeReaction_ShouldThrowException_WhenReactionNotFound() {
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST))
                .thenReturn(Optional.empty());

        assertThrows(LikeNotFoundException.class,
                () -> likeService.changeReaction(10L, 100L, TargetType.POST, ReactionType.LOVE));
    }

    @Test
    void hasReacted_ShouldReturnTrue() {
        when(likeRepository.existsByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST))
                .thenReturn(true);

        boolean result = likeService.hasReacted(10L, 100L, TargetType.POST);

        assertTrue(result);
    }

    @Test
    void getUserReaction_ShouldReturnReaction() {
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST))
                .thenReturn(Optional.of(like));

        LikeResponse response = likeService.getUserReaction(10L, 100L, TargetType.POST);

        assertEquals("LIKE", response.getReactionType());
    }

    @Test
    void getUserReaction_ShouldThrowException_WhenNotFound() {
        when(likeRepository.findByUserIdAndTargetIdAndTargetType(10L, 100L, TargetType.POST))
                .thenReturn(Optional.empty());

        assertThrows(LikeNotFoundException.class,
                () -> likeService.getUserReaction(10L, 100L, TargetType.POST));
    }

    @Test
    void getReactionsByTarget_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Like> page = new PageImpl<>(List.of(like), pageable, 1);

        when(likeRepository.findByTargetIdAndTargetTypeOrderByCreatedAtDesc(100L, TargetType.POST, pageable))
                .thenReturn(page);

        Page<LikeResponse> response = likeService.getReactionsByTarget(100L, TargetType.POST, pageable);

        assertEquals(1, response.getContent().size());
    }

    @Test
    void getReactionsByUser_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Like> page = new PageImpl<>(List.of(like), pageable, 1);

        when(likeRepository.findByUserIdOrderByCreatedAtDesc(10L, pageable)).thenReturn(page);

        Page<LikeResponse> response = likeService.getReactionsByUser(10L, pageable);

        assertEquals(1, response.getContent().size());
    }

    @Test
    void getReactionCount_ShouldReturnCount() {
        when(likeRepository.countByTargetIdAndTargetType(100L, TargetType.POST)).thenReturn(5L);

        long count = likeService.getReactionCount(100L, TargetType.POST);

        assertEquals(5L, count);
    }

    @Test
    void getReactionCountByType_ShouldReturnCount() {
        when(likeRepository.countByTargetIdAndTargetTypeAndReactionType(
                100L, TargetType.POST, ReactionType.LOVE)).thenReturn(2L);

        long count = likeService.getReactionCountByType(100L, TargetType.POST, ReactionType.LOVE);

        assertEquals(2L, count);
    }

    @Test
    void getReactionSummary_ShouldReturnSummary() {
        List<Object[]> rawSummary = List.of(
                new Object[]{ReactionType.LIKE, 3L},
                new Object[]{ReactionType.LOVE, 2L}
        );

        when(likeRepository.getReactionSummaryRaw(100L, TargetType.POST)).thenReturn(rawSummary);

        ReactionSummaryResponse response = likeService.getReactionSummary(100L, TargetType.POST);

        assertEquals(100L, response.getTargetId());
        assertEquals("POST", response.getTargetType());
        assertEquals(5L, response.getTotalCount());
        assertEquals(3L, response.getReactions().get("LIKE"));
        assertEquals(2L, response.getReactions().get("LOVE"));
    }
}