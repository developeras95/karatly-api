package com.karatly.entity.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A precious metal, keyed by its ISO 4217 code (XAU gold, XAG silver, XPT platinum, XPD palladium).
 * Reference data: seeded by Flyway and read-only through the API.
 */
@Entity
@Table(name = "metal")
public class Metal {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "code", length = 3, nullable = false, updatable = false)
    private String code;

    @Column(name = "name", length = 30, nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Metal() {
        // for JPA; metals are created by Flyway seed data only
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
