package com.connectsphere.media.scheduler;

import com.connectsphere.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that expires stories older than 24 hours.
 *
 * Runs every 5 minutes by default (configurable via app.story.cleanup-cron).
 * Per the NFR: stories must be purged within 5 minutes of their expiry time.
 *
 * The job calls MediaService.expireOldStories() which issues a single
 * bulk JPQL UPDATE — efficient even for large story volumes.
 *
 * In a clustered deployment, add ShedLock or use a distributed scheduler
 * to ensure only one instance runs the job at a time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoryExpiryScheduler {

    private final MediaService mediaService;

    /**
     * Expire stories on a cron schedule.
     * Default: every 5 minutes — "0 *\/5 * * * *"
     *
     * The cron expression is read from application.yml so it can be
     * changed without code modifications.
     */
    @Scheduled(cron = "${app.story.cleanup-cron:0 */5 * * * *}")
    public void runStoryExpiryJob() {
        log.debug("Story expiry job started at {}", java.time.LocalDateTime.now());

        try {
            int expired = mediaService.expireOldStories();
            if (expired > 0) {
                log.info("Story expiry job completed: {} stories expired", expired);
            } else {
                log.debug("Story expiry job: no stories to expire");
            }
        } catch (Exception e) {
            log.error("Story expiry job failed: {}", e.getMessage(), e);
        }
    }
}