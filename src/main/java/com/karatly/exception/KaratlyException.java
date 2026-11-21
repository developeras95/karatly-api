package com.karatly.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for errors the API reports on purpose. Each one carries an HTTP status and a
 * stable, machine-readable code (e.g. METAL_NOT_FOUND) that clients can rely on, while the
 * message stays human-readable.
 */
public abstract class KaratlyException extends RuntimeException {

    private final String code;

    protected KaratlyException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public abstract HttpStatus getStatus();
}
