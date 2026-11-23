package com.karatly.dto.reference;

import java.math.BigDecimal;

import com.karatly.entity.reference.Purity;
import com.karatly.util.Decimals;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Purity")
public record PurityResponse(
        @Schema(example = "3") int id,
        @Schema(description = "ISO 4217 metal code", example = "XAU") String metal,
        @Schema(example = "22K (916)") String label,
        @Schema(description = "Share of pure metal, 0 to 1", example = "0.916") BigDecimal fineness) {

    public static PurityResponse from(Purity purity) {
        return new PurityResponse(purity.getId(), purity.getMetalCode(), purity.getLabel(),
                Decimals.plain(purity.getFineness()));
    }
}
