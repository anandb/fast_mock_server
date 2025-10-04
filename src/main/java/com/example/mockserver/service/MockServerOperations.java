package com.example.mockserver.service;

import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RequestDefinition;

/**
 * Interface for MockServer operations to enable better testability.
 * This abstraction layer makes it easier to mock MockServer functionality in tests.
 */
public interface MockServerOperations {

    /**
     * Configure an expectation on the mock server.
     *
     * @param request the HTTP request to match
     * @param response the HTTP response to return
     */
    void configureExpectation(RequestDefinition request, HttpResponse response);

    /**
     * Reset the mock server, clearing all expectations.
     */
    void reset();

    /**
     * Retrieve all active expectations.
     *
     * @param request the request definition to match (can be null for all)
     * @return array of active expectations
     */
    Expectation[] retrieveActiveExpectations(RequestDefinition request);
}
