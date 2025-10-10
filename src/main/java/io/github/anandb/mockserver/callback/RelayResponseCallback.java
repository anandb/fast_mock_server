package io.github.anandb.mockserver.callback;

import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.service.RelayService;
import io.github.anandb.mockserver.service.RelayService.RelayResponse;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Callback for relaying HTTP requests to a remote server with OAuth2 authentication.
 * <p>
 * This callback intercepts all requests to the mock server and forwards them to the
 * configured remote URL, including OAuth2 token acquisition and custom headers.
 * </p>
 */
@Slf4j
public class RelayResponseCallback implements ExpectationResponseCallback {

    private final RelayService relayService;
    private final RelayConfig relayConfig;

    public RelayResponseCallback(RelayService relayService, RelayConfig relayConfig) {
        this.relayService = relayService;
        this.relayConfig = relayConfig;
    }

    @Override
    public HttpResponse handle(HttpRequest httpRequest) {
        try {
            log.debug("Relaying request: {} {}", httpRequest.getMethod().getValue(), httpRequest.getPath().getValue());

            // Extract request details
            String method = httpRequest.getMethod().getValue();
            String path = httpRequest.getPath().getValue();

            // Add query parameters to path if present
            if (httpRequest.getQueryStringParameters() != null && !httpRequest.getQueryStringParameters().isEmpty()) {
                String queryString = httpRequest.getQueryStringParameters().getEntries().stream()
                    .map(param -> param.getName().getValue() + "=" +
                         String.join(",", param.getValues().stream()
                             .map(val -> val.getValue())
                             .collect(Collectors.toList())))
                    .collect(Collectors.joining("&"));
                path = path + "?" + queryString;
            }

            // Convert headers to Map<String, List<String>>
            Map<String, List<String>> headers = null;
            if (httpRequest.getHeaders() != null && !httpRequest.getHeaders().isEmpty()) {
                headers = httpRequest.getHeaders().getEntries().stream()
                    .collect(Collectors.toMap(
                        header -> header.getName().getValue(),
                        header -> header.getValues().stream()
                            .map(val -> val.getValue())
                            .collect(Collectors.toList())
                    ));
            }

            // Get request body
            byte[] body = null;
            if (httpRequest.getBody() != null) {
                String bodyString = httpRequest.getBodyAsString();
                if (bodyString != null && !bodyString.isEmpty()) {
                    body = bodyString.getBytes();
                }
            }

            // Relay the request
            RelayResponse relayResponse = relayService.relayRequest(
                relayConfig,
                method,
                path,
                headers,
                body
            );

            // Build MockServer HttpResponse from relay response
            HttpResponse response = HttpResponse.response()
                .withStatusCode(relayResponse.getStatusCode());

            // Add response headers
            if (relayResponse.getHeaders() != null) {
                for (Map.Entry<String, List<String>> header : relayResponse.getHeaders().entrySet()) {
                    if (header.getValue() != null && !header.getValue().isEmpty()) {
                        response = response.withHeader(header.getKey(),
                            header.getValue().toArray(new String[0]));
                    }
                }
            }

            // Add response body
            if (relayResponse.getBody() != null && relayResponse.getBody().length > 0) {
                response = response.withBody(relayResponse.getBody());
            }

            return response;

        } catch (Exception e) {
            log.error("Error relaying request: {} {}", httpRequest.getMethod().getValue(),
                     httpRequest.getPath().getValue(), e);

            // Return error response
            return HttpResponse.response()
                .withStatusCode(502)
                .withBody("Error relaying request to remote server: " + e.getMessage());
        }
    }
}
