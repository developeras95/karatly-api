package com.karatly.entity.reference;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** How much pure metal an item holds, e.g. 22K gold = 0.916. Reference data seeded by Flyway. */
@Entity
@Table(name = "purity")
public class Purity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Short id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "metal_code", length = 3, nullable = false, updatable = false)
    private String metalCode;

    @Column(name = "label", length = 20, nullable = false)
    private String label;

    @Column(name = "fineness", precision = 5, scale = 4, nullable = false)
    private BigDecimal fineness;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Purity() {
        // for JPA
    }

    public Short getId() {
        return id;
    }

    public String getMetalCode() {
        return metalCode;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getFineness() {
        return fineness;
    }
}
