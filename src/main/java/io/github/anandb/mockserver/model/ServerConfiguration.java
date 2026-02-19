package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Represents a complete server configuration including its creation parameters and expectations.
 * <p>
 * This model is used when loading server configurations from a JSON file at startup.
 * </p>
 */
public class ServerConfiguration {

    /** Server creation parameters */
    @NotNull(message = "Server configuration is required")
    @Valid
    @JsonProperty("server")
    private ServerCreationRequest server;

    /** Optional list of expectations to configure on this server at startup */
    @JsonProperty("expectations")
    private List<EnhancedExpectationDTO> expectations;

    public ServerConfiguration() {}

    public ServerConfiguration(ServerCreationRequest server, List<EnhancedExpectationDTO> expectations) {
        this.server = server;
        this.expectations = expectations;
    }

    public ServerCreationRequest getServer() { return server; }
    public void setServer(ServerCreationRequest server) { this.server = server; }
    public List<EnhancedExpectationDTO> getExpectations() { return expectations; }
    public void setExpectations(List<EnhancedExpectationDTO> expectations) { this.expectations = expectations; }

    /**
     * Checks if this server has any expectations configured.
     *
     * @return true if at least one expectation is configured, false otherwise
     */
    public boolean hasExpectations() {
        return expectations != null && !expectations.isEmpty();
    }
}
