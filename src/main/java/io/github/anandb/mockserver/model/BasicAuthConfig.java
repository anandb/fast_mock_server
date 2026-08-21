package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration for HTTP Basic Authentication.
 * <p>
 * This model defines the credentials required for basic authentication.
 * When configured, the mock server will require all incoming requests to
 * include valid basic authentication credentials.
 * </p>
 */
public class BasicAuthConfig {
    /**
     * Username for basic authentication
     */
    @NotBlank(message = "Username is required for basic authentication")
    @JsonProperty("username")
    private String username;
    /**
     * Password for basic authentication
     */
    @NotBlank(message = "Password is required for basic authentication")
    @JsonProperty("password")
    private String password;

    /**
     * Checks if the basic auth configuration is valid.
     *
     * @return true if both username and password are non-blank, false otherwise
     */
    public boolean isValid() {
        return username != null && !username.isBlank() && password != null && !password.isBlank();
    }

    /**
     * Username for basic authentication
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Password for basic authentication
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Username for basic authentication
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * Password for basic authentication
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof BasicAuthConfig)) return false;
        final BasicAuthConfig other = (BasicAuthConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$username = this.getUsername();
        final Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof BasicAuthConfig;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        return result;
    }

    public BasicAuthConfig() {
    }

    /**
     * Creates a new {@code BasicAuthConfig} instance.
     *
     * @param username Username for basic authentication
     * @param password Password for basic authentication
     */
    public BasicAuthConfig(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String toString() {
        return "BasicAuthConfig(username=" + this.getUsername() + ")";
    }
}
