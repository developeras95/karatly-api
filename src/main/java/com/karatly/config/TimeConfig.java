package com.karatly.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One clock for the whole application. "Today" decides which day's rate a sale uses and how long a
 * lot has been held, so it must not depend on the server's time zone. Tests replace this bean with
 * a fixed clock to control dates.
 */
@Configuration
public class TimeConfig {

    @Bean
    Clock clock(@Value("${karatly.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
