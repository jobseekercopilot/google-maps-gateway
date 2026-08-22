package com.jobseekercopilot.googlemapsgateway.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> invalid(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> disabled(IllegalStateException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<Map<String, Object>> timeout(ResourceAccessException exception) {
        return response(HttpStatus.GATEWAY_TIMEOUT, "Google Maps provider timed out");
    }

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<Map<String, Object>> provider(RestClientResponseException exception) {
        HttpStatus status = switch (exception.getStatusCode().value()) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401, 403 -> HttpStatus.SERVICE_UNAVAILABLE;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            case 504 -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return response(status, "Google Maps provider request failed");
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(), "message", message, "timestamp", Instant.now().toString()));
    }
}
