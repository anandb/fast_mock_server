package io.github.anandb.mockserver.util;

import com.fasterxml.jackson.core.JsonProcessingException;

public class ErrorCode {
    private final String errorCode;
    private final String message;

    public ErrorCode(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    @Override
    public String toString() {
        try {
            return MapperSupplier.getMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
