package com.karatly.entity.rate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The price of one gram of pure metal in one currency on one day, from one source. Fetching again on
 * the same day updates the row, so the table is both the rate history and the cache that keeps calls
 * to the outside APIs down.
 */
@Entity
@Table(name = "metal_rate")
public class MetalRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "metal_code", length = 3, nullable = false, updatable = false)
    private String metalCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", length = 3, nullable = false, updatable = false)
    private String currency;

    @Column(name = "rate_date", nullable = false, updatable = false)
    private LocalDate rateDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20, nullable = false, updatable = false)
    private RateSource source;

    @Column(name = "spot_price_per_gram", precision = 19, scale = 6)
    private BigDecimal spotPricePerGram;

    @Column(name = "premium_percent", precision = 7, scale = 4, nullable = false)
    private BigDecimal premiumPercent;

    @Column(name = "price_per_gram", precision = 19, scale = 6, nullable = false)
    private BigDecimal pricePerGram;

    @Column(name = "priced_at", nullable = false)
    private Instant pricedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MetalRate() {
        // for JPA
    }

    public MetalRate(String metalCode, String currency, LocalDate rateDate, RateSource source, Instant now) {
        this.metalCode = metalCode;
        this.currency = currency;
        this.rateDate = rateDate;
        this.source = source;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Sets the prices. pricePerGram is what Karatly uses; spot and premium record how it was reached. */
    public void setPrices(BigDecimal spotPricePerGram, BigDecimal premiumPercent, BigDecimal pricePerGram,
                          Instant pricedAt, Instant now) {
        this.spotPricePerGram = spotPricePerGram;
        this.premiumPercent = premiumPercent;
        this.pricePerGram = pricePerGram;
        this.pricedAt = pricedAt;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getMetalCode() {
        return metalCode;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getRateDate() {
        return rateDate;
    }

    public RateSource getSource() {
        return source;
    }

    public BigDecimal getSpotPricePerGram() {
        return spotPricePerGram;
    }

    public BigDecimal getPremiumPercent() {
        return premiumPercent;
    }

    public BigDecimal getPricePerGram() {
        return pricePerGram;
    }

    public Instant getPricedAt() {
        return pricedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
