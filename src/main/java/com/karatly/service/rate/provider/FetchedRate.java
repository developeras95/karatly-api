package com.karatly.service.rate.provider;

import java.math.BigDecimal;
import java.time.Instant;

/** A spot price for one gram of pure metal, as a provider returned it, already in the requested currency. */
public record FetchedRate(String metalCode, BigDecimal spotPricePerGram, Instant pricedAt) {
}
