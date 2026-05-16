package com.connectsphere.follow.service;

import com.connectsphere.follow.dto.response.FollowCountResponse;
import com.connectsphere.follow.dto.response.FollowResponse;
import com.connectsphere.follow.entity.Follow;
import com.connectsphere.follow.exception.DuplicateFollowException;
import com.connectsphere.follow.exception.FollowNotFoundException;
import com.connectsphere.follow.exception.SelfFollowException;
import com.connectsphere.follow.repository.FollowRepository;
import com.connectsphere.follow.service.impl.FollowServiceImpl;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private FollowServiceImpl followService;

    private Follow follow;

    @BeforeEach
    void setUp() {
        follow = Follow.builder()
                .id(1L)
                .followerId(10L)
                .followeeId(20L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void follow_ShouldCreateFollow_WhenValid() {
        when(followRepository.existsByFollowerIdAndFolloweeId(10L, 20L)).thenReturn(false);
        when(followRepository.save(any(Follow.class))).thenReturn(follow);

        FollowResponse response = followService.follow(10L, 20L);

        assertNotNull(response);
        assertEquals(10L, response.getFollowerId());
        assertEquals(20L, response.getFolloweeId());

        verify(followRepository).save(any(Follow.class));
    }

    @Test
    void follow_ShouldThrowException_WhenSelfFollow() {
        assertThrows(SelfFollowException.class, () -> followService.follow(10L, 10L));

        verify(followRepository, never()).save(any(Follow.class));
    }

    @Test
    void follow_ShouldThrowException_WhenDuplicateFollow() {
        when(followRepository.existsByFollowerIdAndFolloweeId(10L, 20L)).thenReturn(true);

        assertThrows(DuplicateFollowException.class, () -> followService.follow(10L, 20L));

        verify(followRepository, never()).save(any(Follow.class));
    }

    @Test
    void unfollow_ShouldDeleteFollow_WhenExists() {
        when(followRepository.existsByFollowerIdAndFolloweeId(10L, 20L)).thenReturn(true);

        followService.unfollow(10L, 20L);

        verify(followRepository).deleteByFollowerIdAndFolloweeId(10L, 20L);
    }

    @Test
    void unfollow_ShouldThrowException_WhenFollowNotFound() {
        when(followRepository.existsByFollowerIdAndFolloweeId(10L, 20L)).thenReturn(false);

        assertThrows(FollowNotFoundException.class, () -> followService.unfollow(10L, 20L));

        verify(followRepository, never()).deleteByFollowerIdAndFolloweeId(anyLong(), anyLong());
    }

    @Test
    void isFollowing_ShouldReturnTrue() {
        when(followRepository.existsByFollowerIdAndFolloweeId(10L, 20L)).thenReturn(true);

        boolean result = followService.isFollowing(10L, 20L);

        assertTrue(result);
    }

    @Test
    void getFollowers_ShouldReturnFollowersPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Follow> page = new PageImpl<>(List.of(follow), pageable, 1);

        when(followRepository.findByFolloweeIdOrderByCreatedAtDesc(20L, pageable)).thenReturn(page);

        Page<FollowResponse> response = followService.getFollowers(20L, pageable);

        assertEquals(1, response.getContent().size());
        assertEquals(10L, response.getContent().get(0).getFollowerId());
    }

    @Test
    void getFollowing_ShouldReturnFollowingPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Follow> page = new PageImpl<>(List.of(follow), pageable, 1);

        when(followRepository.findByFollowerIdOrderByCreatedAtDesc(10L, pageable)).thenReturn(page);

        Page<FollowResponse> response = followService.getFollowing(10L, pageable);

        assertEquals(1, response.getContent().size());
        assertEquals(20L, response.getContent().get(0).getFolloweeId());
    }

    @Test
    void getFollowingIds_ShouldReturnIds() {
        when(followRepository.findFolloweeIdsByFollowerId(10L)).thenReturn(List.of(20L, 30L));

        List<Long> ids = followService.getFollowingIds(10L);

        assertEquals(List.of(20L, 30L), ids);
    }

    @Test
    void getFollowerIds_ShouldReturnIds() {
        when(followRepository.findFollowerIdsByFolloweeId(20L)).thenReturn(List.of(10L, 30L));

        List<Long> ids = followService.getFollowerIds(20L);

        assertEquals(List.of(10L, 30L), ids);
    }

    @Test
    void getFollowCounts_ShouldReturnFollowerAndFollowingCounts() {
        when(followRepository.countByFolloweeId(20L)).thenReturn(5L);
        when(followRepository.countByFollowerId(20L)).thenReturn(3L);

        FollowCountResponse response = followService.getFollowCounts(20L);

        assertEquals(20L, response.getUserId());
        assertEquals(5L, response.getFollowerCount());
        assertEquals(3L, response.getFollowingCount());
    }

    @Test
    void getFollowerCount_ShouldReturnCount() {
        when(followRepository.countByFolloweeId(20L)).thenReturn(5L);

        long count = followService.getFollowerCount(20L);

        assertEquals(5L, count);
    }

    @Test
    void getFollowingCount_ShouldReturnCount() {
        when(followRepository.countByFollowerId(10L)).thenReturn(4L);

        long count = followService.getFollowingCount(10L);

        assertEquals(4L, count);
    }

    @Test
    void getMutualFollowIds_ShouldReturnIds() {
        when(followRepository.findMutualFollowIds(10L)).thenReturn(List.of(20L, 30L));

        List<Long> result = followService.getMutualFollowIds(10L);

        assertEquals(List.of(20L, 30L), result);
    }

    @Test
    void getSuggestedUserIds_ShouldReturnIds() {
        when(followRepository.findSuggestedUserIds(eq(10L), any(Pageable.class)))
                .thenReturn(List.of(40L, 50L));

        List<Long> result = followService.getSuggestedUserIds(10L, 10);

        assertEquals(List.of(40L, 50L), result);
    }

    @Test
    void getSuggestedUserIds_ShouldCapLimitAt20() {
        when(followRepository.findSuggestedUserIds(eq(10L), any(Pageable.class)))
                .thenReturn(List.of(40L));

        followService.getSuggestedUserIds(10L, 100);

        verify(followRepository).findSuggestedUserIds(eq(10L), argThat(pageable ->
                pageable.getPageSize() == 20
        ));
    }
}