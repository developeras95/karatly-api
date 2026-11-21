package com.karatly.entity.reference;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A unit of weight. Karatly stores every weight in grams; a unit only records how many grams one
 * of it is, so converting is one exact multiplication.
 */
@Entity
@Table(name = "weight_unit")
public class WeightUnit {

    @Id
    @Column(name = "code", length = 10, nullable = false, updatable = false)
    private String code;

    @Column(name = "name", length = 30, nullable = false)
    private String name;

    @Column(name = "symbol", length = 10, nullable = false)
    private String symbol;

    @Column(name = "grams_per_unit", precision = 20, scale = 10, nullable = false, updatable = false)
    private BigDecimal gramsPerUnit;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected WeightUnit() {
        // for JPA
    }

    public WeightUnit(String code, String name, String symbol, BigDecimal gramsPerUnit, short sortOrder) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.gramsPerUnit = gramsPerUnit;
        this.sortOrder = sortOrder;
        this.active = true;
    }

    /** The same weight in grams, exact (no rounding). */
    public BigDecimal toGrams(BigDecimal quantity) {
        return quantity.multiply(gramsPerUnit);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getGramsPerUnit() {
        return gramsPerUnit;
    }

    public short getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
