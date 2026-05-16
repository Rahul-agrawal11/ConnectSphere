package com.connectsphere.media.scheduler;

import com.connectsphere.media.service.MediaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryExpirySchedulerTest {

    @Mock
    MediaService mediaService;

    @InjectMocks
    StoryExpiryScheduler scheduler;

    @Test
    void runStoryExpiryJob_shouldCallExpireOldStories() {
        when(mediaService.expireOldStories()).thenReturn(5);

        scheduler.runStoryExpiryJob();

        verify(mediaService).expireOldStories();
    }

    @Test
    void runStoryExpiryJob_noneExpired_shouldNotThrow() {
        when(mediaService.expireOldStories()).thenReturn(0);

        scheduler.runStoryExpiryJob();

        verify(mediaService).expireOldStories();
    }

    @Test
    void runStoryExpiryJob_serviceThrows_shouldSwallowException() {
        when(mediaService.expireOldStories()).thenThrow(new RuntimeException("DB down"));

        // Scheduler must not let an exception kill the thread
        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> scheduler.runStoryExpiryJob());
    }
}