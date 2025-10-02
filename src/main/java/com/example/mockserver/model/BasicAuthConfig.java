package com.example.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuration for HTTP Basic Authentication.
 * <p>
 * This model defines the credentials required for basic authentication.
 * When configured, the mock server will require all incoming requests to
 * include valid basic authentication credentials.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasicAuthConfig {

    /** Username for basic authentication */
    @NotBlank(message = "Username is required for basic authentication")
    @JsonProperty("username")
    private String username;

    /** Password for basic authentication */
    @NotBlank(message = "Password is required for basic authentication")
    @JsonProperty("password")
    private String password;

    /**
     * Checks if the basic auth configuration is valid.
     *
     * @return true if both username and password are non-blank, false otherwise
     */
    public boolean isValid() {
        return username != null && !username.isBlank() &&
               password != null && !password.isBlank();
    }
}
