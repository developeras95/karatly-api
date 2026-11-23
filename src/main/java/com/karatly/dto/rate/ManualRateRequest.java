package com.karatly.dto.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@Schema(name = "ManualRateRequest", description = "A rate typed in by hand, e.g. the day's IBJA rate. "
        + "It wins over fetched rates for that day.")
public record ManualRateRequest(
        @Schema(example = "XAU") @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a three-letter code such as XAU")
        String metal,
        @Schema(description = "Defaults to today") @PastOrPresent LocalDate date,
        @Schema(description = "Per gram of pure metal, in the app's currency", example = "12850.00")
        @NotNull @Positive @Digits(integer = 13, fraction = 6) BigDecimal pricePerGram) {
}
