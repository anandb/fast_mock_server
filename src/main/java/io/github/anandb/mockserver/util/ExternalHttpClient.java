package io.github.anandb.mockserver.util;

import io.github.anandb.mockserver.model.HttpResponseDTO;
import java.util.Map;

/**
 * Represents a client used to communicate with external HTTP services during relaying.
 * This interface defines methods needed for mocking in RelayServiceTest.
 */
public interface ExternalHttpClient {

    /**
     * Executes an HTTP request against an external target URI and returns the response.
     * @param headers The required headers for the outgoing request.
     * @return An HttpResponseDTO containing the received status, body, and headers.
     * @throws RuntimeException if connection fails or timeout occurs.
     */
    HttpResponseDTO executeRequest(String method, java.net.URI uri, Map<String, String> headers) throws Exception;
}