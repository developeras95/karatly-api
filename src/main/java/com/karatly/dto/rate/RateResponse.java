package com.karatly.dto.rate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.karatly.entity.rate.RateSource;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Rate", description = "The latest rate of one metal, per gram of pure metal")
public record RateResponse(
        @Schema(example = "XAU") String metal,
        @Schema(example = "Gold") String metalName,
        @Schema(example = "INR") String currency,
        @Schema(description = "Day the rate is for, in the app's time zone") LocalDate date,
        @Schema(description = "The rate Karatly uses: spot × (1 + premium)", example = "11955.27") BigDecimal pricePerGram,
        @Schema(description = "International spot price; empty for a manual rate", example = "10811.76")
        BigDecimal spotPricePerGram,
        @Schema(description = "Premium over spot applied, in percent", example = "10.5") BigDecimal premiumPercent,
        RateSource source,
        @Schema(description = "When the source priced it") Instant pricedAt,
        @Schema(description = "True when the newest rate is from an earlier day") boolean stale,
        @Schema(description = "The rate for each purity of this metal") List<PurityPrice> purities) {
}
