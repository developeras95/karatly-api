package com.karatly.service.rate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MetalUnitsAndJsonValuesTest {

    private static final Instant FALLBACK = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void convertsEveryUnitToGrams() {
        assertThat(MetalUnits.perGram(new BigDecimal("31.1034768"), "troy_ounce")).isEqualByComparingTo("1");
        assertThat(MetalUnits.perGram(new BigDecimal("453.59237"), "pound")).isEqualByComparingTo("1");
        assertThat(MetalUnits.perGram(new BigDecimal("1000"), "kg")).isEqualByComparingTo("1");
        assertThat(MetalUnits.perGram(new BigDecimal("7.5"), "gram")).isEqualByComparingTo("7.5");
    }

    @Test
    void rejectsAnUnknownUnit() {
        assertThatThrownBy(() -> MetalUnits.perGram(BigDecimal.ONE, "tonne")).isInstanceOf(RateFetchException.class);
        assertThatThrownBy(() -> MetalUnits.perGram(BigDecimal.ONE, null)).isInstanceOf(RateFetchException.class);
    }

    @Test
    void readsNumbersGivenAsTextOrNumbers() {
        assertThat(JsonValues.decimal(Map.of("price", "42.689"), "price", "test")).isEqualByComparingTo("42.689");
        assertThat(JsonValues.decimal(Map.of("price", 42.5), "price", "test")).isEqualByComparingTo("42.5");
        assertThatThrownBy(() -> JsonValues.decimal(Map.of(), "price", "test")).hasMessageContaining("no 'price'");
        assertThatThrownBy(() -> JsonValues.decimal(Map.of("price", "n/a"), "price", "test"))
                .hasMessageContaining("not a number");
    }

    @Test
    void readsTimesInEveryForm() {
        assertThat(JsonValues.time("2026-11-23T06:05:57Z", FALLBACK)).isEqualTo(Instant.parse("2026-11-23T06:05:57Z"));
        assertThat(JsonValues.time(1795400000L, FALLBACK)).isEqualTo(Instant.ofEpochSecond(1795400000L));
        assertThat(JsonValues.time(1795400000123L, FALLBACK)).isEqualTo(Instant.ofEpochMilli(1795400000123L));
        assertThat(JsonValues.time("yesterday", FALLBACK)).isEqualTo(FALLBACK);
        assertThat(JsonValues.time(null, FALLBACK)).isEqualTo(FALLBACK);
    }
}
