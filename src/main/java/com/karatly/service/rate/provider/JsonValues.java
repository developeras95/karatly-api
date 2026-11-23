package com.karatly.service.rate.provider;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Reads values out of a provider's JSON, parsed as plain maps, and fails with a clear message. */
final class JsonValues {

    private JsonValues() {
    }

    /** A price given as a JSON number or as a decimal string ("42.689"). */
    static BigDecimal decimal(Map<?, ?> json, String field, String context) {
        Object value = json.get(field);
        if (value == null) {
            throw new RateFetchException(context + ": the answer has no '" + field + "'.");
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            throw new RateFetchException(context + ": '" + field + "' is not a number: " + value + ".");
        }
    }

    /** An ISO-8601 time ("2026-11-23T05:51:02.019+00:00"), or now when missing or unreadable. */
    static Instant time(Object value, Instant fallback) {
        if (value instanceof String text) {
            try {
                return OffsetDateTime.parse(text).toInstant();
            } catch (DateTimeParseException ex) {
                return fallback;
            }
        }
        if (value instanceof Number number) {
            long epoch = number.longValue();
            // Seconds or milliseconds since 1970: anything past the year 33658 in seconds is milliseconds.
            return epoch > 1_000_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
        }
        return fallback;
    }
}
