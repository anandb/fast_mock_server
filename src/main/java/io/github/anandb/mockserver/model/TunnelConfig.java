package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TunnelConfig {

    @NotBlank(message = "Namespace is required for tunnel configuration")
    @JsonProperty("namespace")
    private String namespace;

    @NotBlank(message = "Pod prefix is required for tunnel configuration")
    @JsonProperty("podPrefix")
    private String podPrefix;

    @NotNull(message = "Pod port is required for tunnel configuration")
    @JsonProperty("podPort")
    private Integer podPort;

    /** Max time in ms to wait for tunnel to become ready before failing. Default: 10s. */
    @JsonProperty("tunnelReadyTimeoutMs")
    private long tunnelReadyTimeoutMs = 10_000;

    /** 3-arg constructor for backward compatibility (uses default timeout). */
    public TunnelConfig(String namespace, String podPrefix, Integer podPort) {
        this.namespace = namespace;
        this.podPrefix = podPrefix;
        this.podPort = podPort;
        this.tunnelReadyTimeoutMs = 10_000;
    }
}
