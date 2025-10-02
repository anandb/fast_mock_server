package com.example.mockserver.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleServerNotFound(ServerNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            "SERVER_NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ServerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleServerAlreadyExists(ServerAlreadyExistsException ex) {
        ErrorResponse error = new ErrorResponse(
            "SERVER_ALREADY_EXISTS",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidCertificateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCertificate(InvalidCertificateException ex) {
        ErrorResponse error = new ErrorResponse(
            "INVALID_CERTIFICATE",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ServerCreationException.class)
    public ResponseEntity<ErrorResponse> handleServerCreation(ServerCreationException ex) {
        ErrorResponse error = new ErrorResponse(
            "SERVER_CREATION_FAILED",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(InvalidExpectationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidExpectation(InvalidExpectationException ex) {
        ErrorResponse error = new ErrorResponse(
            "INVALID_EXPECTATION",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse response = new ValidationErrorResponse(
            "VALIDATION_FAILED",
            "Request validation failed",
            errors,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred: " + ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Standard error response
     */
    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private LocalDateTime timestamp;
    }

    /**
     * Validation error response with field-level errors
     */
    @Data
    @AllArgsConstructor
    public static class ValidationErrorResponse {
        private String errorCode;
        private String message;
        private Map<String, String> fieldErrors;
        private LocalDateTime timestamp;
    }
}
