package com.karatly.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for fetching metal rates. Premiums per metal live in the database instead
 * (/api/v1/rates/premiums), because they change at runtime.
 *
 * @param currency       currency rates are fetched and stored in, e.g. INR
 * @param fetchesPerDay  how often a day rates are fetched, for every metal. The cache lifetime (TTL) is
 *                       24 hours / fetchesPerDay: a provider is called only when a cached rate of one of
 *                       its metals was last updated longer ago than that
 * @param schedulerEnabled whether the background job runs
 * @param checkCron      how often the job checks the cache (it only calls providers whose rates are past the TTL)
 * @param refreshOnRead  on a read, also fetch rates that are past the TTL
 * @param retryAfter     how long to wait before calling a provider again after it failed
 * @param goldpriceDev   goldprice.dev: gold, silver, copper
 * @param goldapiIo      goldapi.io: platinum, palladium
 */
@ConfigurationProperties(prefix = "karatly.rates")
public record RatesProperties(
        String currency,
        int fetchesPerDay,
        boolean schedulerEnabled,
        String checkCron,
        boolean refreshOnRead,
        Duration retryAfter,
        Provider goldpriceDev,
        Provider goldapiIo) {

    public RatesProperties {
        if (fetchesPerDay < 1 || fetchesPerDay > 96) {
            throw new IllegalArgumentException("karatly.rates.fetches-per-day must be between 1 and 96, not " + fetchesPerDay);
        }
    }

    /** How long a cached rate stays fresh: 24 hours / fetchesPerDay (4 → 6 hours). */
    public Duration ttl() {
        return Duration.ofDays(1).dividedBy(fetchesPerDay);
    }

    /**
     * @param enabled       switch the provider off without removing its key
     * @param baseUrl       API address
     * @param apiKey        API key; goldprice.dev also answers without one, goldapi.io does not
     * @param monthlyBudget most HTTP requests Karatly may use in a calendar month (keep under the plan's quota)
     * @param timeout       connect and read timeout
     */
    public record Provider(boolean enabled, String baseUrl, String apiKey, int monthlyBudget, Duration timeout) {

        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
