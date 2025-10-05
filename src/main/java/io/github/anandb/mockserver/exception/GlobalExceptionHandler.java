package io.github.anandb.mockserver.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers.
 * <p>
 * Provides centralized exception handling for all REST endpoints, converting exceptions
 * into appropriate HTTP responses with consistent error structures.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ServerNotFoundException by returning a 404 NOT FOUND response.
     *
     * @param ex the ServerNotFoundException thrown
     * @return ResponseEntity with error details and 404 status
     */
    @ExceptionHandler(ServerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleServerNotFound(ServerNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            "SERVER_NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles ServerAlreadyExistsException by returning a 409 CONFLICT response.
     *
     * @param ex the ServerAlreadyExistsException thrown
     * @return ResponseEntity with error details and 409 status
     */
    @ExceptionHandler(ServerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleServerAlreadyExists(ServerAlreadyExistsException ex) {
        ErrorResponse error = new ErrorResponse(
            "SERVER_ALREADY_EXISTS",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles InvalidCertificateException by returning a 400 BAD REQUEST response.
     *
     * @param ex the InvalidCertificateException thrown
     * @return ResponseEntity with error details and 400 status
     */
    @ExceptionHandler(InvalidCertificateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCertificate(InvalidCertificateException ex) {
        ErrorResponse error = new ErrorResponse(
            "INVALID_CERTIFICATE",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles ServerCreationException by returning a 500 INTERNAL SERVER ERROR response.
     *
     * @param ex the ServerCreationException thrown
     * @return ResponseEntity with error details and 500 status
     */
    @ExceptionHandler(ServerCreationException.class)
    public ResponseEntity<ErrorResponse> handleServerCreation(ServerCreationException ex) {
        ErrorResponse error = new ErrorResponse(
            "SERVER_CREATION_FAILED",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handles InvalidExpectationException by returning a 400 BAD REQUEST response.
     *
     * @param ex the InvalidExpectationException thrown
     * @return ResponseEntity with error details and 400 status
     */
    @ExceptionHandler(InvalidExpectationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidExpectation(InvalidExpectationException ex) {
        ErrorResponse error = new ErrorResponse(
            "INVALID_EXPECTATION",
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles MethodArgumentNotValidException (validation errors) by returning a 400 BAD REQUEST response.
     * <p>
     * Extracts field-level validation errors and returns them in a structured format.
     * </p>
     *
     * @param ex the MethodArgumentNotValidException thrown
     * @return ResponseEntity with field validation errors and 400 status
     */
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

    /**
     * Handles HttpMessageNotReadableException (malformed JSON) by returning a 400 BAD REQUEST response.
     * <p>
     * This exception is thrown when the request body cannot be read or parsed as valid JSON.
     * </p>
     *
     * @param ex the HttpMessageNotReadableException thrown
     * @return ResponseEntity with error details and 400 status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse error = new ErrorResponse(
            "MALFORMED_REQUEST",
            "Malformed JSON request: " + ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles all other uncaught exceptions by returning a 500 INTERNAL SERVER ERROR response.
     * <p>
     * This is a catch-all handler for unexpected errors not covered by more specific handlers.
     * </p>
     *
     * @param ex the Exception thrown
     * @return ResponseEntity with error details and 500 status
     */
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
     * Standard error response structure for API errors.
     * <p>
     * Contains an error code, descriptive message, and timestamp of when the error occurred.
     * </p>
     */
    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        /** The error code identifying the type of error */
        private String errorCode;
        /** A human-readable description of the error */
        private String message;
        /** The timestamp when the error occurred */
        private LocalDateTime timestamp;
    }

    /**
     * Validation error response structure for validation failures.
     * <p>
     * Extends the standard error response with field-level validation errors,
     * mapping each field name to its validation error message.
     * </p>
     */
    @Data
    @AllArgsConstructor
    public static class ValidationErrorResponse {
        /** The error code identifying this as a validation failure */
        private String errorCode;
        /** A human-readable description of the validation failure */
        private String message;
        /** Map of field names to their specific validation error messages */
        private Map<String, String> fieldErrors;
        /** The timestamp when the validation error occurred */
        private LocalDateTime timestamp;
    }
}
