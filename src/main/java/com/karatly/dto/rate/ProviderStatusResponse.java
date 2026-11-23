package com.karatly.dto.rate;

import java.time.Instant;
import java.util.List;

import com.karatly.entity.rate.RateSource;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProviderStatus", description = "A rate provider's settings and how much of its quota is used")
public record ProviderStatusResponse(
        RateSource provider,
        List<String> metals,
        boolean available,
        @Schema(description = "Why it is unavailable, if it is") String unavailableReason,
        @Schema(description = "Cache lifetime in minutes: 24 h / karatly.rates.fetches-per-day", example = "360")
        long ttlMinutes,
        int requestsThisMonth,
        int monthlyBudget,
        Instant lastFetchAt,
        Boolean lastFetchSucceeded,
        @Schema(description = "When the oldest cached rate of this provider passes the TTL") Instant nextFetchDue) {
}
