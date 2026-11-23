package com.karatly.dto.rate;

import java.util.List;

import com.karatly.entity.rate.RateSource;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RefreshResult", description = "What happened when one provider was asked for rates")
public record RefreshResult(RateSource provider, Status status, List<String> metals, String message) {

    public enum Status {
        /** New rates were fetched and stored. */
        FETCHED,
        /** Not due: every cached rate is younger than the TTL, or waiting after a failure. */
        NOT_DUE,
        /** Switched off or missing its API key. */
        UNAVAILABLE,
        /** The fetch would go over the monthly budget. */
        OVER_BUDGET,
        /** The provider failed; the previous rates stay. */
        FAILED
    }
}
