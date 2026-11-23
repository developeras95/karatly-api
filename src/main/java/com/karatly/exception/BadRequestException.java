package com.karatly.exception;

import org.springframework.http.HttpStatus;

/** The request is understood but its values do not make sense together. Maps to 400. */
public class BadRequestException extends KaratlyException {

    public BadRequestException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
