package com.karatly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Scheduled jobs. Each job has its own switch (karatly.rates.scheduler-enabled for the daily
 * rate fetch). The tests turn it off, so that no job can fire in the middle of a test run.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
