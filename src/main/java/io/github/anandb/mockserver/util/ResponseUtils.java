package io.github.anandb.mockserver.util;

import io.github.anandb.mockserver.model.GlobalHeader;
import org.mockserver.model.Header;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.NottableString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.mockserver.model.Header.header;

/**
 * Utility class for common HTTP response processing tasks.
 */
public final class ResponseUtils {
    /**
     * Merges global headers into a MockServer response.
     * Global headers are only added if the response doesn't already contain a header with the same name.
     *
     * @param response the HttpResponse to merge headers into
     * @param globalHeaders the list of global headers
     * @return the updated HttpResponse
     */
    public static HttpResponse mergeGlobalHeaders(HttpResponse response, List<GlobalHeader> globalHeaders) {
        if (globalHeaders == null || globalHeaders.isEmpty()) {
            return response;
        }
        List<Header> existingHeaders = response.getHeaderList() != null ? new ArrayList<>(response.getHeaderList()) : new ArrayList<>();
        java.util.Set<String> existingNames = existingHeaders.stream().map(Header::getName).filter(java.util.Objects::nonNull).map(NottableString::getValue).filter(java.util.Objects::nonNull).map(String::toLowerCase).collect(Collectors.toSet());
        List<Header> mergedHeaders = new ArrayList<>(existingHeaders);
        for (GlobalHeader globalHeader : globalHeaders) {
            if (globalHeader.getName() != null) {
                String lowerName = globalHeader.getName().toLowerCase();
                if (!existingNames.contains(lowerName)) {
                    mergedHeaders.add(header(globalHeader.getName(), globalHeader.getValue()));
                    existingNames.add(lowerName);
                }
            }
        }
        return response.withHeaders(mergedHeaders);
    }

    private ResponseUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
