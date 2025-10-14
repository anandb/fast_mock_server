package io.github.anandb.mockserver.callback;

import io.github.anandb.mockserver.service.FreemarkerTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

/**
 * Callback for processing Freemarker templates in response bodies.
 * <p>
 * This callback is invoked by MockServer when a request matches an expectation
 * configured with a Freemarker template. It processes the template with the
 * incoming request context (headers, body, cookies, path variables) and returns the rendered response.
 * </p>
 */
@Slf4j
public class FreemarkerResponseCallback implements ExpectationResponseCallback {

    private final FreemarkerTemplateService templateService;
    private final String templateString;
    private final HttpResponse baseResponse;
    private final String pathPattern;

    public FreemarkerResponseCallback(
            FreemarkerTemplateService templateService,
            String templateString,
            HttpResponse baseResponse,
            String pathPattern) {
        this.templateService = templateService;
        this.templateString = templateString;
        this.baseResponse = baseResponse;
        this.pathPattern = pathPattern;
    }

    @Override
    public HttpResponse handle(HttpRequest httpRequest) {
        try {
            // Process the template with the incoming request and path pattern
            String processedBody = templateService.processTemplateWithRequest(
                templateString,
                httpRequest,
                pathPattern
            );

            // Return response with processed body and original headers/status
            return HttpResponse.response()
                    .withStatusCode(baseResponse.getStatusCode())
                    .withHeaders(baseResponse.getHeaderList())
                    .withBody(processedBody);
        } catch (Exception e) {
            log.error("Error processing Freemarker template", e);
            // Return error response
            return HttpResponse.response()
                    .withStatusCode(500)
                    .withBody("Error processing template: " + e.getMessage());
        }
    }
}
