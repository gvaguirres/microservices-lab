package org.example.bff.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<String> handleHttpClientError(HttpClientErrorException ex) {

        log.warn("Upstream service error: {} - {}", ex.getStatusCode(), ex.getStatusText());

        HttpStatusCode statusCode = ex.getStatusCode();
        String responseBody = ex.getResponseBodyAsString();

        return ResponseEntity
                .status(statusCode)
                .contentType(ex.getResponseHeaders().getContentType())
                .body(responseBody);
    }
}
