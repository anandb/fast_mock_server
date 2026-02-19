package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.callback.EnhancedResponseCallback;
import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RequestDefinition;

import java.util.List;

/**
 * Concrete implementation of MockServerOperations that wraps the actual MockServer ClientAndServer.
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
    public void configureEnhancedExpectation(EnhancedExpectationDTO config, List<GlobalHeader> globalHeaders, List<ResponseStrategy> strategies) {
        String pathPattern = null;
        HttpRequest request = config.getHttpRequest();
        if (request != null && request.getPath() != null) {
            pathPattern = request.getPath().getValue();
        }

        EnhancedResponseCallback callback = new EnhancedResponseCallback(
                config,
                globalHeaders,
                strategies,
                pathPattern
        );

        clientAndServer.when(config.getHttpRequest()).respond(callback);
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
