package io.github.anandb.mockserver.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HttpStatus Enum Tests")
class HttpStatusTest {

    @Test
    void okReturns200() {
        assertEquals(200, HttpStatus.OK.getCode());
    }

    @Test
    void createdReturns201() {
        assertEquals(201, HttpStatus.CREATED.getCode());
    }

    @Test
    void badRequestReturns400() {
        assertEquals(400, HttpStatus.BAD_REQUEST.getCode());
    }

    @Test
    void unauthorizedReturns401() {
        assertEquals(401, HttpStatus.UNAUTHORIZED.getCode());
    }

    @Test
    void forbiddenReturns403() {
        assertEquals(403, HttpStatus.FORBIDDEN.getCode());
    }

    @Test
    void notFoundReturns404() {
        assertEquals(404, HttpStatus.NOT_FOUND.getCode());
    }

    @Test
    void methodNotAllowedReturns405() {
        assertEquals(405, HttpStatus.METHOD_NOT_ALLOWED.getCode());
    }

    @Test
    void conflictReturns409() {
        assertEquals(409, HttpStatus.CONFLICT.getCode());
    }

    @Test
    void internalServerErrorReturns500() {
        assertEquals(500, HttpStatus.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    void badGatewayReturns502() {
        assertEquals(502, HttpStatus.BAD_GATEWAY.getCode());
    }

    @Test
    void allValuesCovered() {
        assertEquals(10, HttpStatus.values().length);
    }

    @Test
    void valueOfMatchesName() {
        assertEquals(HttpStatus.OK, HttpStatus.valueOf("OK"));
        assertEquals(HttpStatus.NOT_FOUND, HttpStatus.valueOf("NOT_FOUND"));
    }
}
