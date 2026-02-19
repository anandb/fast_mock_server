package io.github.anandb.mockserver.util;

import io.github.anandb.mockserver.model.GlobalHeader;
import lombok.experimental.UtilityClass;
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
@UtilityClass
public class ResponseUtils {

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

        List<Header> existingHeaders = response.getHeaderList() != null
                ? new ArrayList<>(response.getHeaderList())
                : new ArrayList<>();

        Map<NottableString, Header> headerMap = existingHeaders.stream().collect(Collectors.toMap(
                Header::getName,
                h -> h,
                (h1, h2) -> h1
        ));

        for (GlobalHeader globalHeader : globalHeaders) {
            NottableString headerName = NottableString.string(globalHeader.getName());
            if (!headerMap.containsKey(headerName)) {
                headerMap.put(headerName, header(globalHeader.getName(), globalHeader.getValue()));
            }
        }

        return response.withHeaders(new ArrayList<>(headerMap.values()));
    }
}
