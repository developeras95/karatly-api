package com.karatly.dto.rate;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PurityPrice", description = "The rate for one gram of an item of this purity")
public record PurityPrice(
        @Schema(example = "22K (916)") String label,
        @Schema(example = "0.916") BigDecimal fineness,
        @Schema(example = "10951.02") BigDecimal pricePerGram) {
}
