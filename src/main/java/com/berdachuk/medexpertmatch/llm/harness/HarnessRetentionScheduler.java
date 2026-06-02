package com.berdachuk.medexpertmatch.llm.harness;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HarnessRetentionScheduler {

    private final HarnessRetentionService retentionService;

    public HarnessRetentionScheduler(HarnessRetentionService retentionService) {
        this.retentionService = retentionService;
    }

    @Scheduled(cron = "${medexpertmatch.harness.retention.cron:0 0 3 * * ?}")
    public void purgeExpiredRuns() {
        log.info("Starting scheduled harness retention purge");
        try {
            int purged = retentionService.purgeExpiredRuns();
            log.info("Harness retention purge completed: {} rows deleted", purged);
        } catch (Exception e) {
            log.warn("Harness retention purge failed: {}", e.getMessage());
        }
    }
}
