package org.example.bff.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<String> handleHttpClientError(HttpClientErrorException ex) {

        HttpStatusCode statusCode = ex.getStatusCode();
        String responseBody = ex.getResponseBodyAsString();

        return ResponseEntity
                .status(statusCode)
                .body(responseBody);
    }
}
