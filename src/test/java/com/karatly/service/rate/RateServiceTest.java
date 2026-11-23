package com.karatly.service.rate;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RateServiceTest {

    @Test
    void appliesThePremiumAndRoundsToSixDecimals() {
        assertThat(RateService.withPremium(new BigDecimal("10000"), new BigDecimal("10")))
                .isEqualByComparingTo("11000.000000");
        assertThat(RateService.withPremium(new BigDecimal("127.411735"), new BigDecimal("6.25")))
                .isEqualByComparingTo("135.374968");
        assertThat(RateService.withPremium(new BigDecimal("1.5"), BigDecimal.ZERO)).isEqualByComparingTo("1.5");
    }
}
