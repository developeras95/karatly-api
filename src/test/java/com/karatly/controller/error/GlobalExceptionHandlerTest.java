package com.karatly.controller.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.karatly.dto.error.FieldViolation;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Handlers for request bodies. No endpoint takes a body yet (purchases and sales will), so these are
 * called directly to pin down the response shape the web app relies on.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

    @Test
    void invalidBodyListsEveryFieldAndObjectError() throws Exception {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "purchase");
        errors.addError(new FieldError("purchase", "netWeight", "must be greater than 0"));
        errors.addError(new ObjectError("purchase", "net weight cannot exceed gross weight"));
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleEndpoint", Object.class), 0);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(parameter, errors), new HttpHeaders(), HttpStatus.BAD_REQUEST,
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getProperties()).containsEntry("code", "VALIDATION_FAILED");
        assertThat(body.getProperties()).containsEntry("errors", List.of(
                new FieldViolation("netWeight", "must be greater than 0"),
                new FieldViolation("purchase", "net weight cannot exceed gross weight")));
    }

    @Test
    void unreadableBodyIsReportedAsMalformed() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("bad JSON", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Object> response =
                handler.handleHttpMessageNotReadable(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        ProblemDetail body = (ProblemDetail) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getProperties()).containsEntry("code", "MALFORMED_REQUEST");
    }

    @SuppressWarnings("unused")
    private void sampleEndpoint(Object body) {
        // only its parameter is used, to build a MethodArgumentNotValidException
    }
}
