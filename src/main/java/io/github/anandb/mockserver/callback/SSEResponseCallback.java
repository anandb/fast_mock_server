package io.github.anandb.mockserver.callback;

import lombok.extern.slf4j.Slf4j;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.List;

/**
 * Callback for serving Server-Sent Events (SSE) responses.
 * <p>
 * This callback is invoked by MockServer when a request matches an expectation
 * configured for SSE. It sends multiple messages with a specified interval between them,
 * following the SSE protocol format.
 * </p>
 */
@Slf4j
public class SSEResponseCallback implements ExpectationResponseCallback {

    private final List<String> messages;
    private final int intervalMs;
    private final HttpResponse baseResponse;

    /**
     * Creates a new SSE response callback.
     *
     * @param messages the list of messages to send
     * @param intervalMs the interval in milliseconds between messages
     * @param baseResponse the base HTTP response (contains headers and status)
     */
    public SSEResponseCallback(List<String> messages, int intervalMs, HttpResponse baseResponse) {
        this.messages = messages;
        this.intervalMs = intervalMs;
        this.baseResponse = baseResponse;
    }

    @Override
    public HttpResponse handle(HttpRequest httpRequest) {
        try {
            log.info("Handling SSE request for {} {} with {} messages at {}ms intervals",
                    httpRequest.getMethod(), httpRequest.getPath(), messages.size(), intervalMs);

            // Build SSE response body
            StringBuilder sseBody = new StringBuilder();

            for (int i = 0; i < messages.size(); i++) {
                String message = messages.get(i);

                // Format as SSE event
                sseBody.append("data: ").append(message).append("\n\n");

                // Add delay simulation note (actual delay would need to be handled by streaming)
                if (i < messages.size() - 1) {
                    log.debug("SSE message {}/{}: {}", i + 1, messages.size(), message);
                }
            }

            // Note: In a real SSE implementation, messages would be sent with actual delays.
            // MockServer's callback model doesn't support streaming responses with delays,
            // so we send all messages immediately but format them as SSE events.
            // The client should still parse them correctly as separate SSE events.

            log.info("Prepared SSE response with {} events", messages.size());

            // Build response with SSE headers
            return HttpResponse.response()
                    .withStatusCode(baseResponse.getStatusCode() != null ? baseResponse.getStatusCode() : 200)
                    .withHeaders(baseResponse.getHeaderList())
                    .withHeader("Content-Type", "text/event-stream")
                    .withHeader("Cache-Control", "no-cache")
                    .withHeader("Connection", "keep-alive")
                    .withBody(sseBody.toString());

        } catch (Exception e) {
            log.error("Error creating SSE response", e);
            return HttpResponse.response()
                    .withStatusCode(500)
                    .withBody("Error creating SSE response: " + e.getMessage());
        }
    }
}
