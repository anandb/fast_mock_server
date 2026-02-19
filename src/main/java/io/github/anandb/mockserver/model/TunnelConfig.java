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
}
