package com.karatly.dto.rate;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

@Schema(name = "PremiumRequest")
public record PremiumRequest(
        @Schema(description = "Percent added to the international spot price, e.g. import duty", example = "6")
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 4) BigDecimal premiumPercent) {
}
