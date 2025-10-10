package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * Configuration for relaying requests to a remote server with OAuth2 authentication.
 * <p>
 * When a server has relay configuration enabled, all incoming requests will be
 * forwarded to the specified remote URL after obtaining an OAuth2 access token.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelayConfig {

    /** The remote URL to relay requests to */
    @NotBlank(message = "Remote URL is required for relay configuration")
    @JsonProperty("remoteUrl")
    private String remoteUrl;

    /** The OAuth2 token endpoint URL */
    @NotBlank(message = "Token URL is required for relay configuration")
    @JsonProperty("tokenUrl")
    private String tokenUrl;

    /** OAuth2 client ID */
    @NotBlank(message = "Client ID is required for relay configuration")
    @JsonProperty("clientId")
    private String clientId;

    /** OAuth2 client secret */
    @NotBlank(message = "Client secret is required for relay configuration")
    @JsonProperty("clientSecret")
    private String clientSecret;

    /** Optional additional headers to include when relaying requests */
    @JsonProperty("headers")
    private Map<String, String> headers;

    /** Optional OAuth2 scope */
    @JsonProperty("scope")
    private String scope;

    /** Optional OAuth2 grant type (defaults to client_credentials) */
    @JsonProperty("grantType")
    private String grantType = "client_credentials";

    /**
     * Checks if this relay configuration is valid.
     *
     * @return true if all required fields are present, false otherwise
     */
    public boolean isValid() {
        return remoteUrl != null && !remoteUrl.isBlank() &&
               tokenUrl != null && !tokenUrl.isBlank() &&
               clientId != null && !clientId.isBlank() &&
               clientSecret != null && !clientSecret.isBlank();
    }

    /**
     * Checks if custom headers are configured.
     *
     * @return true if headers are present and not empty, false otherwise
     */
    public boolean hasHeaders() {
        return headers != null && !headers.isEmpty();
    }
}
