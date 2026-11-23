package com.karatly.dto.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.karatly.entity.rate.RateSource;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RateHistory", description = "One rate per day, oldest first; days without a rate are left out")
public record RateHistoryResponse(String metal, String currency, LocalDate from, LocalDate to, List<Point> points) {

    @Schema(name = "RateHistoryPoint")
    public record Point(LocalDate date, BigDecimal pricePerGram, RateSource source) {
    }
}
