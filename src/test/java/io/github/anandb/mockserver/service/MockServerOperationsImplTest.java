package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.callback.EnhancedResponseCallback;
import io.github.anandb.mockserver.model.EnhancedExpectation;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.ForwardChainExpectation;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RequestDefinition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MockServerOperationsImpl Tests")
class MockServerOperationsImplTest {

    @Mock
    private ClientAndServer clientAndServer;
    @Mock
    private ForwardChainExpectation chain;

    private MockServerOperationsImpl operations;

    @BeforeEach
    void setUp() {
        operations = new MockServerOperationsImpl(clientAndServer);
        lenient().when(clientAndServer.when(any())).thenReturn(chain);
        lenient().when(chain.withPriority(anyInt())).thenReturn(chain);
    }

    @Test
    void configureExpectationDelegatesToClientAndServer() {
        RequestDefinition request = mock(RequestDefinition.class);
        HttpResponse response = mock(HttpResponse.class);

        operations.configureExpectation(request, response);

        verify(clientAndServer).when(request);
        verify(chain).respond(response);
    }

    @Test
    void resetDelegatesToClientAndServer() {
        operations.reset();
        verify(clientAndServer).reset();
    }

    @Test
    void retrieveActiveExpectationsDelegatesToClientAndServer() {
        RequestDefinition request = mock(RequestDefinition.class);
        Expectation[] expected = new Expectation[]{mock(Expectation.class)};
        when(clientAndServer.retrieveActiveExpectations(request)).thenReturn(expected);

        Expectation[] result = operations.retrieveActiveExpectations(request);

        assertSame(expected, result);
        verify(clientAndServer).retrieveActiveExpectations(request);
    }

    @Test
    void configureEnhancedExpectationWithNullPath() {
        EnhancedExpectation config = new EnhancedExpectation();
        config.setHttpRequest(null);

        List<GlobalHeader> headers = List.of();
        List<ResponseStrategy> strategies = List.of();
        List<RelayConfig> relays = List.of();

        // Should not throw even with null http request
        operations.configureEnhancedExpectation(config, headers, strategies, relays);

        verify(clientAndServer).when((RequestDefinition) isNull());
        verify(chain).respond(any(EnhancedResponseCallback.class));
    }

    @Test
    void configureEnhancedExpectationWithPriority() {
        EnhancedExpectation config = new EnhancedExpectation();
        config.setHttpRequest(null);

        List<GlobalHeader> headers = List.of();
        List<ResponseStrategy> strategies = List.of();
        List<RelayConfig> relays = List.of();

        operations.configureEnhancedExpectation(config, headers, strategies, relays, 5);

        verify(clientAndServer).when((RequestDefinition) isNull());
        verify(chain).withPriority(5);
        verify(chain).respond(any(EnhancedResponseCallback.class));
    }

    @Test
    void configureEnhancedExpectationDelegatesToThreeArgOverload() {
        EnhancedExpectation config = new EnhancedExpectation();
        config.setHttpRequest(null);

        List<GlobalHeader> headers = List.of();
        List<ResponseStrategy> strategies = List.of();
        List<RelayConfig> relays = List.of();

        // The 4-arg version should call the 5-arg version with priority=0
        operations.configureEnhancedExpectation(config, headers, strategies, relays);

        verify(clientAndServer).when((RequestDefinition) isNull());
        verify(chain).withPriority(0);
        verify(chain).respond(any(EnhancedResponseCallback.class));
    }
}
