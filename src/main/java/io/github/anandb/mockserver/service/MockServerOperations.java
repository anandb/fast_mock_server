package io.github.anandb.mockserver.service;

import io.github.anandb.mockserver.model.EnhancedExpectationDTO;
import io.github.anandb.mockserver.model.GlobalHeader;
import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.strategy.ResponseStrategy;
import org.mockserver.mock.Expectation;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RequestDefinition;

import java.util.List;

/**
 * Interface defining operations that can be performed on a MockServer instance.
 */
public interface MockServerOperations {

    /**
     * Configures a standard expectation on the mock server.
     *
     * @param request the request matcher
     * @param response the response to return
     */
    void configureExpectation(RequestDefinition request, HttpResponse response);

    /**
     * Configures an enhanced expectation using the universal callback and strategies.
     *
     * @param config the enhanced expectation configuration
     * @param globalHeaders global headers to apply
     * @param strategies list of available response strategies
     * @param relays list of relay configurations
     */
    void configureEnhancedExpectation(EnhancedExpectationDTO config, List<GlobalHeader> globalHeaders, List<ResponseStrategy> strategies, List<RelayConfig> relays);


    /**
     * Resets the mock server, clearing all expectations.
     */
    void reset();

    /**
     * Retrieves all active expectations matching the given request.
     *
     * @param request the request matcher
     * @return an array of active expectations
     */
    Expectation[] retrieveActiveExpectations(RequestDefinition request);
}
