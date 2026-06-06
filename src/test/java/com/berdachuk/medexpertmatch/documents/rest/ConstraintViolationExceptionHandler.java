package com.berdachuk.medexpertmatch.documents.rest;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Test-only controller advice that maps {@link ConstraintViolationException} to HTTP 400
 * for standalone MockMvc tests of {@code @Validated} REST controllers. Standalone MockMvc
 * setup does not register the default Spring exception resolvers that translate bean
 * validation failures, so this advice bridges that gap.
 */
@RestControllerAdvice
public class ConstraintViolationExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handle(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
