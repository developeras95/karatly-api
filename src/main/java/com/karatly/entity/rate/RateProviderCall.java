package com.karatly.entity.rate;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One fetch from an outside rate API: how many HTTP requests it used and whether it worked. */
@Entity
@Table(name = "rate_provider_call")
public class RateProviderCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", length = 20, nullable = false, updatable = false)
    private String provider;

    @Column(name = "called_at", nullable = false, updatable = false)
    private Instant calledAt;

    @Column(name = "requests", nullable = false, updatable = false)
    private short requests;

    @Column(name = "success", nullable = false, updatable = false)
    private boolean success;

    @Column(name = "message", length = 500, updatable = false)
    private String message;

    protected RateProviderCall() {
        // for JPA
    }

    public RateProviderCall(String provider, Instant calledAt, int requests, boolean success, String message) {
        this.provider = provider;
        this.calledAt = calledAt;
        this.requests = (short) requests;
        this.success = success;
        this.message = message == null || message.length() <= 500 ? message : message.substring(0, 500);
    }

    public String getProvider() {
        return provider;
    }

    public Instant getCalledAt() {
        return calledAt;
    }

    public int getRequests() {
        return requests;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
