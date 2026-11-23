package com.karatly.entity.rate;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Premium over the international spot price for one metal, in percent (e.g. Indian import duty).
 * Applied when a rate is fetched; the rate row keeps the premium it was fetched with.
 */
@Entity
@Table(name = "rate_premium")
public class RatePremium {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "metal_code", length = 3, nullable = false, updatable = false)
    private String metalCode;

    @Column(name = "premium_percent", precision = 7, scale = 4, nullable = false)
    private BigDecimal premiumPercent;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RatePremium() {
        // for JPA; one row per metal is seeded by Flyway
    }

    public void change(BigDecimal premiumPercent, Instant now) {
        this.premiumPercent = premiumPercent;
        this.updatedAt = now;
    }

    public String getMetalCode() {
        return metalCode;
    }

    public BigDecimal getPremiumPercent() {
        return premiumPercent;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
