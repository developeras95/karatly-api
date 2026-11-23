package com.karatly.util;

import java.math.BigDecimal;

/** Small helpers for showing BigDecimal values. */
public final class Decimals {

    private Decimals() {
    }

    /** Without trailing zeros and never in exponent form: 10.0000 becomes 10, not 1E+1. */
    public static BigDecimal plain(BigDecimal value) {
        return value == null ? null : new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
