package io.github.anandb.mockserver.service;

import org.mockserver.integration.ClientAndServer;
import org.springframework.stereotype.Component;

/**
 * Factory for creating {@link MockServerOperations} instances.
 * <p>
 * Centralizes the creation of {@link MockServerOperationsImpl} so callers
 * don't depend on the concrete implementation directly.
 * </p>
 */
@Component
public class MockServerOperationsFactory {

    /**
     * Creates a new {@link MockServerOperations} for the given MockServer instance.
     *
     * @param clientAndServer the MockServer client to wrap
     * @return a new MockServerOperations instance
     */
    public MockServerOperations create(ClientAndServer clientAndServer) {
        return new MockServerOperationsImpl(clientAndServer);
    }
}
