package com.karatly.entity.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class WeightUnitTest {

    @Test
    void convertsToGramsExactly() {
        WeightUnit tola = new WeightUnit("TOLA", "Tola", "tola", new BigDecimal("11.6638038"), (short) 5);

        assertThat(tola.toGrams(new BigDecimal("2.5"))).isEqualByComparingTo("29.1595095");
    }

    @Test
    void newUnitIsActive() {
        WeightUnit gram = new WeightUnit("G", "Gram", "g", BigDecimal.ONE, (short) 1);

        assertThat(gram.isActive()).isTrue();
        assertThat(gram.getSortOrder()).isEqualTo((short) 1);
    }
}
