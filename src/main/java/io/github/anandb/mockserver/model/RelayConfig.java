package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
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
public class RelayConfig {
    /**
     * The remote URL to relay requests to
     */
    @JsonProperty("remoteUrl")
    private String remoteUrl;
    /**
     * The prefixes to match against the request path (ant patterns)
     */
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
    @JsonProperty(value = "clientSecret", access = JsonProperty.Access.WRITE_ONLY)
    private String clientSecret;
    /**
     * Optional additional headers to include when relaying requests
     */
    @JsonProperty("headers")
    private Map<String, String> headers;
    /**
     * Optional OAuth2 scope
     */
    @JsonProperty("scope")
    private String scope;
    /**
     * Optional OAuth2 grant type (defaults to client_credentials)
     */
    @JsonProperty("grantType")
    private String grantType = "client_credentials";
    /**
     * Optional tunnel configuration for Kubernetes port-forwarding
     */
    @Valid
    @JsonProperty("tunnelConfig")
    private TunnelConfig tunnelConfig;
    /**
     * The assigned host port for the tunnel (populated at runtime)
     */
    @JsonProperty("assignedHostPort")
    private Integer assignedHostPort;
    /**
     * Whether to ignore SSL certificate errors when relaying requests or fetching tokens
     */
    @JsonProperty("ignoreSSLErrors")
    private boolean ignoreSSLErrors = false;

    /**
     * Checks if OAuth2 authentication is enabled for this relay configuration.
     *
     * @return true if all OAuth2 fields are present, false otherwise
     */
    public boolean isOAuth2Enabled() {
        return tokenUrl != null && !tokenUrl.isBlank() && clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    /**
     * Checks if tunnel configuration is enabled for this relay.
     *
     * @return true if tunnel configuration is present and valid, false otherwise
     */
    public boolean isTunnelEnabled() {
        return tunnelConfig != null && tunnelConfig.getNamespace() != null && !tunnelConfig.getNamespace().isBlank() && tunnelConfig.getPodPrefix() != null && !tunnelConfig.getPodPrefix().isBlank() && tunnelConfig.getPodPort() != null;
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

    /**
     * The remote URL to relay requests to
     */
    public String getRemoteUrl() {
        return this.remoteUrl;
    }

    /**
     * The prefixes to match against the request path (ant patterns)
     */
    public List<String> getPrefixes() {
        return this.prefixes;
    }

    /**
     * The OAuth2 token endpoint URL (optional - only required if using OAuth2
     * authentication)
     */
    public String getTokenUrl() {
        return this.tokenUrl;
    }

    /**
     * OAuth2 client ID (optional - only required if using OAuth2 authentication)
     */
    public String getClientId() {
        return this.clientId;
    }

    /**
     * OAuth2 client secret (optional - only required if using OAuth2
     * authentication)
     */
    public String getClientSecret() {
        return this.clientSecret;
    }

    /**
     * Optional additional headers to include when relaying requests
     */
    public Map<String, String> getHeaders() {
        return this.headers;
    }

    /**
     * Optional OAuth2 scope
     */
    public String getScope() {
        return this.scope;
    }

    /**
     * Optional OAuth2 grant type (defaults to client_credentials)
     */
    public String getGrantType() {
        return this.grantType;
    }

    /**
     * Optional tunnel configuration for Kubernetes port-forwarding
     */
    public TunnelConfig getTunnelConfig() {
        return this.tunnelConfig;
    }

    /**
     * The assigned host port for the tunnel (populated at runtime)
     */
    public Integer getAssignedHostPort() {
        return this.assignedHostPort;
    }

    /**
     * Whether to ignore SSL certificate errors when relaying requests or fetching tokens
     */
    public boolean isIgnoreSSLErrors() {
        return this.ignoreSSLErrors;
    }

    /**
     * The remote URL to relay requests to
     */
    public void setRemoteUrl(final String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    /**
     * The prefixes to match against the request path (ant patterns)
     */
    public void setPrefixes(final List<String> prefixes) {
        this.prefixes = prefixes;
    }

    /**
     * The OAuth2 token endpoint URL (optional - only required if using OAuth2
     * authentication)
     */
    public void setTokenUrl(final String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    /**
     * OAuth2 client ID (optional - only required if using OAuth2 authentication)
     */
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    /**
     * OAuth2 client secret (optional - only required if using OAuth2
     * authentication)
     */
    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * Optional additional headers to include when relaying requests
     */
    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Optional OAuth2 scope
     */
    public void setScope(final String scope) {
        this.scope = scope;
    }

    /**
     * Optional OAuth2 grant type (defaults to client_credentials)
     */
    public void setGrantType(final String grantType) {
        this.grantType = grantType;
    }

    /**
     * Optional tunnel configuration for Kubernetes port-forwarding
     */
    public void setTunnelConfig(final TunnelConfig tunnelConfig) {
        this.tunnelConfig = tunnelConfig;
    }

    /**
     * The assigned host port for the tunnel (populated at runtime)
     */
    public void setAssignedHostPort(final Integer assignedHostPort) {
        this.assignedHostPort = assignedHostPort;
    }

    /**
     * Whether to ignore SSL certificate errors when relaying requests or fetching tokens
     */
    public void setIgnoreSSLErrors(final boolean ignoreSSLErrors) {
        this.ignoreSSLErrors = ignoreSSLErrors;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof RelayConfig)) return false;
        final RelayConfig other = (RelayConfig) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.isIgnoreSSLErrors() != other.isIgnoreSSLErrors()) return false;
        final Object this$assignedHostPort = this.getAssignedHostPort();
        final Object other$assignedHostPort = other.getAssignedHostPort();
        if (this$assignedHostPort == null ? other$assignedHostPort != null : !this$assignedHostPort.equals(other$assignedHostPort)) return false;
        final Object this$remoteUrl = this.getRemoteUrl();
        final Object other$remoteUrl = other.getRemoteUrl();
        if (this$remoteUrl == null ? other$remoteUrl != null : !this$remoteUrl.equals(other$remoteUrl)) return false;
        final Object this$prefixes = this.getPrefixes();
        final Object other$prefixes = other.getPrefixes();
        if (this$prefixes == null ? other$prefixes != null : !this$prefixes.equals(other$prefixes)) return false;
        final Object this$tokenUrl = this.getTokenUrl();
        final Object other$tokenUrl = other.getTokenUrl();
        if (this$tokenUrl == null ? other$tokenUrl != null : !this$tokenUrl.equals(other$tokenUrl)) return false;
        final Object this$clientId = this.getClientId();
        final Object other$clientId = other.getClientId();
        if (this$clientId == null ? other$clientId != null : !this$clientId.equals(other$clientId)) return false;
        final Object this$clientSecret = this.getClientSecret();
        final Object other$clientSecret = other.getClientSecret();
        if (this$clientSecret == null ? other$clientSecret != null : !this$clientSecret.equals(other$clientSecret)) return false;
        final Object this$headers = this.getHeaders();
        final Object other$headers = other.getHeaders();
        if (this$headers == null ? other$headers != null : !this$headers.equals(other$headers)) return false;
        final Object this$scope = this.getScope();
        final Object other$scope = other.getScope();
        if (this$scope == null ? other$scope != null : !this$scope.equals(other$scope)) return false;
        final Object this$grantType = this.getGrantType();
        final Object other$grantType = other.getGrantType();
        if (this$grantType == null ? other$grantType != null : !this$grantType.equals(other$grantType)) return false;
        final Object this$tunnelConfig = this.getTunnelConfig();
        final Object other$tunnelConfig = other.getTunnelConfig();
        if (this$tunnelConfig == null ? other$tunnelConfig != null : !this$tunnelConfig.equals(other$tunnelConfig)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof RelayConfig;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isIgnoreSSLErrors() ? 79 : 97);
        final Object $assignedHostPort = this.getAssignedHostPort();
        result = result * PRIME + ($assignedHostPort == null ? 43 : $assignedHostPort.hashCode());
        final Object $remoteUrl = this.getRemoteUrl();
        result = result * PRIME + ($remoteUrl == null ? 43 : $remoteUrl.hashCode());
        final Object $prefixes = this.getPrefixes();
        result = result * PRIME + ($prefixes == null ? 43 : $prefixes.hashCode());
        final Object $tokenUrl = this.getTokenUrl();
        result = result * PRIME + ($tokenUrl == null ? 43 : $tokenUrl.hashCode());
        final Object $clientId = this.getClientId();
        result = result * PRIME + ($clientId == null ? 43 : $clientId.hashCode());
        final Object $clientSecret = this.getClientSecret();
        result = result * PRIME + ($clientSecret == null ? 43 : $clientSecret.hashCode());
        final Object $headers = this.getHeaders();
        result = result * PRIME + ($headers == null ? 43 : $headers.hashCode());
        final Object $scope = this.getScope();
        result = result * PRIME + ($scope == null ? 43 : $scope.hashCode());
        final Object $grantType = this.getGrantType();
        result = result * PRIME + ($grantType == null ? 43 : $grantType.hashCode());
        final Object $tunnelConfig = this.getTunnelConfig();
        result = result * PRIME + ($tunnelConfig == null ? 43 : $tunnelConfig.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "RelayConfig(remoteUrl=" + this.getRemoteUrl() + ", prefixes=" + this.getPrefixes() + ", tokenUrl=" + this.getTokenUrl() + ", clientId=" + this.getClientId() + ", headers=" + this.getHeaders() + ", scope=" + this.getScope() + ", grantType=" + this.getGrantType() + ", tunnelConfig=" + this.getTunnelConfig() + ", assignedHostPort=" + this.getAssignedHostPort() + ", ignoreSSLErrors=" + this.isIgnoreSSLErrors() + ")";
    }

    public RelayConfig() {
    }

    /**
     * Creates a new {@code RelayConfig} instance.
     *
     * @param remoteUrl The remote URL to relay requests to
     * @param prefixes The prefixes to match against the request path (ant patterns)
     * @param tokenUrl The OAuth2 token endpoint URL (optional - only required if using OAuth2
     * authentication)
     * @param clientId OAuth2 client ID (optional - only required if using OAuth2 authentication)
     * @param clientSecret OAuth2 client secret (optional - only required if using OAuth2
     * authentication)
     * @param headers Optional additional headers to include when relaying requests
     * @param scope Optional OAuth2 scope
     * @param grantType Optional OAuth2 grant type (defaults to client_credentials)
     * @param tunnelConfig Optional tunnel configuration for Kubernetes port-forwarding
     * @param assignedHostPort The assigned host port for the tunnel (populated at runtime)
     * @param ignoreSSLErrors Whether to ignore SSL certificate errors when relaying requests or fetching tokens
     */
    public RelayConfig(final String remoteUrl, final List<String> prefixes, final String tokenUrl, final String clientId, final String clientSecret, final Map<String, String> headers, final String scope, final String grantType, final TunnelConfig tunnelConfig, final Integer assignedHostPort, final boolean ignoreSSLErrors) {
        this.remoteUrl = remoteUrl;
        this.prefixes = prefixes;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.headers = headers;
        this.scope = scope;
        this.grantType = grantType;
        this.tunnelConfig = tunnelConfig;
        this.assignedHostPort = assignedHostPort;
        this.ignoreSSLErrors = ignoreSSLErrors;
    }
}
