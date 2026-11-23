package com.karatly.service.rate.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Converts a price per troy ounce, pound or kilogram into a price per gram. */
public final class MetalUnits {

    /** Exact, by international definition. */
    public static final BigDecimal GRAMS_PER_TROY_OUNCE = new BigDecimal("31.1034768");
    /** Exact, by international definition (avoirdupois pound, used for copper). */
    public static final BigDecimal GRAMS_PER_POUND = new BigDecimal("453.59237");

    private static final int SCALE = 6;

    private MetalUnits() {
    }

    public static BigDecimal perGram(BigDecimal price, String unit) {
        String normalised = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        BigDecimal grams = switch (normalised) {
            case "troy_ounce", "oz", "toz" -> GRAMS_PER_TROY_OUNCE;
            case "pound", "lb" -> GRAMS_PER_POUND;
            case "kilogram", "kg" -> BigDecimal.valueOf(1000);
            case "gram", "g" -> BigDecimal.ONE;
            default -> throw new RateFetchException("Unknown price unit '" + unit + "'.");
        };
        return price.divide(grams, SCALE, RoundingMode.HALF_UP);
    }
}
