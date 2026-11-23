package com.karatly.repository.rate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.karatly.entity.rate.MetalRate;
import com.karatly.entity.rate.RateSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetalRateRepository extends JpaRepository<MetalRate, Long> {

    Optional<MetalRate> findByMetalCodeAndCurrencyAndRateDateAndSource(
            String metalCode, String currency, LocalDate rateDate, RateSource source);

    /** The newest rows for a metal; enough to find the latest day and pick its preferred source. */
    List<MetalRate> findTop10ByMetalCodeAndCurrencyOrderByRateDateDescUpdatedAtDesc(String metalCode, String currency);

    List<MetalRate> findByMetalCodeAndCurrencyAndRateDateBetweenOrderByRateDateAsc(
            String metalCode, String currency, LocalDate from, LocalDate to);

    /** The most recently updated rate of a metal from a source: the cache entry the TTL is checked against. */
    Optional<MetalRate> findFirstByMetalCodeAndCurrencyAndSourceOrderByUpdatedAtDesc(
            String metalCode, String currency, RateSource source);
}
