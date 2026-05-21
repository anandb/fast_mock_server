package io.github.anandb.mockserver.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleServerNotFoundReturns404() {
        ServerNotFoundException ex = new ServerNotFoundException("test-server");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleServerNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("SERVER_NOT_FOUND", response.getBody().getErrorCode());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleServerAlreadyExistsReturns409() {
        ServerAlreadyExistsException ex = new ServerAlreadyExistsException("dup-server");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleServerAlreadyExists(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("SERVER_ALREADY_EXISTS", response.getBody().getErrorCode());
    }

    @Test
    void handleInvalidCertificateReturns400() {
        InvalidCertificateException ex = new InvalidCertificateException("bad cert");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleInvalidCertificate(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_CERTIFICATE", response.getBody().getErrorCode());
    }

    @Test
    void handleServerCreationReturns500() {
        ServerCreationException ex = new ServerCreationException("creation failed");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleServerCreation(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("SERVER_CREATION_FAILED", response.getBody().getErrorCode());
    }

    @Test
    void handleInvalidExpectationReturns400() {
        InvalidExpectationException ex = new InvalidExpectationException("bad expectation");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleInvalidExpectation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_EXPECTATION", response.getBody().getErrorCode());
    }

    @Test
    void handleValidationErrorsReturns400() {
        BindingResult bindingResult = org.mockito.Mockito.mock(BindingResult.class);
        org.mockito.Mockito.when(bindingResult.getAllErrors())
                .thenReturn(List.of(new FieldError("obj", "field1", "must not be blank")));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_FAILED", response.getBody().getErrorCode());
        assertEquals("must not be blank", response.getBody().getFieldErrors().get("field1"));
    }

    @Test
    void handleHttpMessageNotReadableReturns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("malformed JSON");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MALFORMED_REQUEST", response.getBody().getErrorCode());
    }

    @Test
    void handleGenericExceptionReturns500() {
        Exception ex = new RuntimeException("unexpected error");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
    }
}
