package com.karatly.dto.reference;

import java.math.BigDecimal;

import com.karatly.entity.reference.WeightUnit;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WeightUnit")
public record WeightUnitResponse(
        @Schema(example = "TOLA") String code,
        @Schema(example = "Tola") String name,
        @Schema(example = "tola") String symbol,
        @Schema(description = "How many grams one of this unit is", example = "11.6638038") BigDecimal gramsPerUnit) {

    public static WeightUnitResponse from(WeightUnit unit) {
        return new WeightUnitResponse(unit.getCode(), unit.getName(), unit.getSymbol(), plain(unit.getGramsPerUnit()));
    }

    /** 1000.0000000000 becomes 1000, not 1E+3, so JSON shows a plain number. */
    private static BigDecimal plain(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
