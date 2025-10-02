package com.example.mockserver.service;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RequestDefinition;

/**
 * Concrete implementation of MockServerOperations that wraps the actual MockServer ClientAndServer.
 * This class is instantiated with a specific ClientAndServer instance when needed.
 */
public class MockServerOperationsImpl implements MockServerOperations {

    private final ClientAndServer clientAndServer;

    public MockServerOperationsImpl(ClientAndServer clientAndServer) {
        this.clientAndServer = clientAndServer;
    }

    @Override
    public void configureExpectation(RequestDefinition request, HttpResponse response) {
        clientAndServer.when(request).respond(response);
    }

    @Override
    public void reset() {
        clientAndServer.reset();
    }

    @Override
    public Expectation[] retrieveActiveExpectations(RequestDefinition request) {
        return clientAndServer.retrieveActiveExpectations(request);
    }
}
