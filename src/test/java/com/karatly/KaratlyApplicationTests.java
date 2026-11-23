package com.karatly;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Checks that the application starts and every Flyway migration ran with its seed data. */
class KaratlyApplicationTests extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationsCreateSchemaAndSeedReferenceData() {
        Integer metals = jdbc.queryForObject("SELECT count(*) FROM metal", Integer.class);
        Integer units = jdbc.queryForObject("SELECT count(*) FROM weight_unit", Integer.class);
        Integer purities = jdbc.queryForObject("SELECT count(*) FROM purity", Integer.class);
        Integer premiums = jdbc.queryForObject("SELECT count(*) FROM rate_premium", Integer.class);
        BigDecimal troyOunce = jdbc.queryForObject(
                "SELECT grams_per_unit FROM weight_unit WHERE code = 'OZT'", BigDecimal.class);

        assertThat(metals).isEqualTo(5);
        assertThat(purities).isEqualTo(11);
        assertThat(premiums).isEqualTo(5);
        assertThat(units).isEqualTo(5);
        assertThat(troyOunce).isEqualByComparingTo("31.1034768");
    }
}
