package com.karatly.dto.rate;

import java.math.BigDecimal;
import java.time.Instant;

import com.karatly.entity.rate.RatePremium;
import com.karatly.util.Decimals;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Premium", description = "Premium over the international spot price, applied from the next fetch")
public record PremiumResponse(@Schema(example = "XAU") String metal, @Schema(example = "6") BigDecimal premiumPercent,
                              Instant updatedAt) {

    public static PremiumResponse from(RatePremium premium) {
        return new PremiumResponse(premium.getMetalCode(), Decimals.plain(premium.getPremiumPercent()),
                premium.getUpdatedAt());
    }
}
