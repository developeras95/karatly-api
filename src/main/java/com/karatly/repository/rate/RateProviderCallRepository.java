package com.karatly.repository.rate;

import java.time.Instant;
import java.util.Optional;

import com.karatly.entity.rate.RateProviderCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RateProviderCallRepository extends JpaRepository<RateProviderCall, Long> {

    /** HTTP requests a provider has used since the given moment (the start of the month). */
    @Query("SELECT COALESCE(SUM(c.requests), 0) FROM RateProviderCall c WHERE c.provider = :provider AND c.calledAt >= :since")
    long sumRequestsSince(String provider, Instant since);

    Optional<RateProviderCall> findFirstByProviderOrderByCalledAtDesc(String provider);
}
