package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
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
    @JsonProperty("remoteUrl")
    private String remoteUrl;

    /** The prefixes to match against the request path (ant patterns) */
    @JsonProperty("prefixes")
    private List<String> prefixes = new ArrayList<>(List.of("/**"));

    public List<String> getAllPrefixes() {
        return (prefixes != null && !prefixes.isEmpty()) ? prefixes : List.of("/**");
    }

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

    /** Optional tunnel configuration for Kubernetes port-forwarding */
    @Valid
    @JsonProperty("tunnelConfig")
    private TunnelConfig tunnelConfig;

    /** The assigned host port for the tunnel (populated at runtime) */
    @JsonProperty("assignedHostPort")
    private Integer assignedHostPort;

    /** Whether to ignore SSL certificate errors when relaying requests or fetching tokens */
    @JsonProperty("ignoreSSLErrors")
    private boolean ignoreSSLErrors = false;

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
     * Checks if tunnel configuration is enabled for this relay.
     *
     * @return true if tunnel configuration is present and valid, false otherwise
     */
    public boolean isTunnelEnabled() {
        return tunnelConfig != null &&
                tunnelConfig.getNamespace() != null && !tunnelConfig.getNamespace().isBlank() &&
                tunnelConfig.getPodPrefix() != null && !tunnelConfig.getPodPrefix().isBlank() &&
                tunnelConfig.getPodPort() != null;
    }

    /**
     * Checks if this relay configuration is valid.
     * A configuration is valid if it has either a remote URL or tunnel configuration.
     * If OAuth2 fields are provided, they must all be complete.
     *
     * @return true if configuration is valid, false otherwise
     */
    public boolean isValid() {
        boolean hasRemoteUrl = remoteUrl != null && !remoteUrl.isBlank();
        boolean hasTunnelConfig = isTunnelEnabled();

        // Must have either remote URL or tunnel config
        if (!hasRemoteUrl && !hasTunnelConfig) {
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

        return true;
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
