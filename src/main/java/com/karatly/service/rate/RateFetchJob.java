package com.karatly.service.rate;

import java.util.List;

import com.karatly.dto.rate.RefreshResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Checks the cache every few minutes (karatly.rates.check-cron) and fetches from providers whose cached
 * rates are past the TTL (24 h / karatly.rates.fetches-per-day).
 */
@Component
@ConditionalOnProperty(prefix = "karatly.rates", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class RateFetchJob {

    private static final Logger log = LoggerFactory.getLogger(RateFetchJob.class);

    private final RateService service;

    public RateFetchJob(RateService service) {
        this.service = service;
    }

    @Scheduled(cron = "${karatly.rates.check-cron}", zone = "${karatly.time-zone}")
    public void run() {
        List<RefreshResult> results = service.refreshIfDue();
        for (RefreshResult result : results) {
            if (result.status() == RefreshResult.Status.FAILED || result.status() == RefreshResult.Status.OVER_BUDGET) {
                log.warn("Scheduled rate fetch from {}: {} ({})", result.provider(), result.status(), result.message());
            }
        }
    }
}
