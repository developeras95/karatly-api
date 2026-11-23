package com.karatly.service.rate.provider;

/** A provider could not deliver rates: unreachable, refused the key, over quota or answered unexpectedly. */
public class RateFetchException extends RuntimeException {

    public RateFetchException(String message) {
        super(message);
    }

    public RateFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
