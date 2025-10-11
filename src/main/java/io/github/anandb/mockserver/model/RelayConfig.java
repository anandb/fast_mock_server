package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Configuration for relaying requests to a remote server with optional OAuth2
 * authentication.
 * <p>
 * When a server has relay configuration enabled, all incoming requests will be
 * forwarded to the specified remote URL. If OAuth2 is configured, an access
 * token
 * will be obtained and added to the request.
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

    /**
     * The OAuth2 token endpoint URL (optional - only required if using OAuth2
     * authentication)
     */
    @JsonProperty("tokenUrl")
    private String tokenUrl;

    /**
     * OAuth2 client ID (optional - only required if using OAuth2 authentication)
     */
    @JsonProperty("clientId")
    private String clientId;

    /**
     * OAuth2 client secret (optional - only required if using OAuth2
     * authentication)
     */
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
     * Checks if OAuth2 authentication is enabled for this relay configuration.
     *
     * @return true if all OAuth2 fields are present, false otherwise
     */
    public boolean isOAuth2Enabled() {
        return tokenUrl != null && !tokenUrl.isBlank() &&
                clientId != null && !clientId.isBlank() &&
                clientSecret != null && !clientSecret.isBlank();
    }

    /**
     * Checks if this relay configuration is valid.
     * A configuration is valid if it has a remote URL, and if OAuth2 fields are
     * provided,
     * they must all be complete.
     *
     * @return true if configuration is valid, false otherwise
     */
    public boolean isValid() {
        // Remote URL is always required
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return false;
        }

        // If any OAuth2 field is provided, all must be provided
        boolean hasTokenUrl = tokenUrl != null && !tokenUrl.isBlank();
        boolean hasClientId = clientId != null && !clientId.isBlank();
        boolean hasClientSecret = clientSecret != null && !clientSecret.isBlank();

        // Either all OAuth2 fields are present or none are present
        if (hasTokenUrl || hasClientId || hasClientSecret) {
            return hasTokenUrl && hasClientId && hasClientSecret;
        }

        return true; // Valid if remote URL is present and no partial OAuth2 config
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
