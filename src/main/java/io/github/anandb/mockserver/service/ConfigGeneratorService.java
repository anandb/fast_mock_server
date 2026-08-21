package io.github.anandb.mockserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Generates template server configuration files from built-in templates.
 * <p>
 * Templates are stored as classpath resources under {@code config-templates/}.
 * Each template corresponds to a specific server feature (basic, TLS, relay, etc.).
 * </p>
 */
@Component
public class ConfigGeneratorService {
    private static final Logger log = LoggerFactory.getLogger(ConfigGeneratorService.class);
    private static final String TEMPLATE_DIR = "config-templates/";
    private static final String TEMPLATE_SUFFIX = ".json";
    /**
     * Map of template type name → human-readable description
     */
    private static final Map<String, String> TEMPLATE_DESCRIPTIONS = new LinkedHashMap<>();

    static {
        TEMPLATE_DESCRIPTIONS.put("simple", "Basic API mock server");
        TEMPLATE_DESCRIPTIONS.put("multi", "HTTP + HTTPS multi-server with TLS");
        TEMPLATE_DESCRIPTIONS.put("basicauth", "Basic authentication (username/password)");
        TEMPLATE_DESCRIPTIONS.put("mtls", "Mutual TLS (mTLS) with client certs");
        TEMPLATE_DESCRIPTIONS.put("pathvars", "Path variables and FreeMarker templates");
        TEMPLATE_DESCRIPTIONS.put("sse", "Server-Sent Events (SSE) streaming");
        TEMPLATE_DESCRIPTIONS.put("relay", "Relay proxy with OAuth2 authentication");
        TEMPLATE_DESCRIPTIONS.put("relay-no-auth", "Relay proxy without authentication");
        TEMPLATE_DESCRIPTIONS.put("files", "File download server");
        TEMPLATE_DESCRIPTIONS.put("files-inline", "File inline server (serve content as body)");
        TEMPLATE_DESCRIPTIONS.put("uploads", "File upload server (multipart/form-data)");
        TEMPLATE_DESCRIPTIONS.put("tunnel", "Kubernetes pod tunnel relay");
    }

    /**
     * Returns the set of available template type names.
     *
     * @return set of template type keys
     */
    public Set<String> getAvailableTypes() {
        return TEMPLATE_DESCRIPTIONS.keySet();
    }

    /**
     * Returns the description for a given template type.
     *
     * @param type the template type
     * @return the description, or null if unknown
     */
    public String getDescription(String type) {
        return TEMPLATE_DESCRIPTIONS.get(type);
    }

    /**
     * Loads and returns the content of a template by type.
     *
     * @param type the template type
     * @return the template content as a string
     * @throws IOException if reading the template fails
     * @throws IllegalArgumentException if the template type is unknown
     */
    public String loadTemplate(String type) throws IOException {
        String templateResource = TEMPLATE_DIR + type + TEMPLATE_SUFFIX;
        ClassPathResource resource = new ClassPathResource(templateResource);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Unknown config type: \'" + type + "\'. Available types: " + String.join(", ", getAvailableTypes()));
        }
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Generates a config file from a template.
     *
     * @param type the template type (e.g. "test", "relay", "mtls")
     * @param outputPath the file path to write the generated config
     * @throws IOException if reading the template or writing the output fails
     * @throws IllegalArgumentException if the template type is unknown
     */
    public void generate(String type, String outputPath) throws IOException {
        String content = loadTemplate(type);
        Path outPath = Paths.get(outputPath);
        if (outPath.getParent() != null) {
            Files.createDirectories(outPath.getParent());
        }
        Files.writeString(outPath, content, StandardCharsets.UTF_8);
        log.info("Generated config type \'{}\' → {}", type, outPath.toAbsolutePath());
    }

    /**
     * Prints usage information to stdout.
     */
    public void printUsage() {
        System.out.println("mock-server — Mock servers, relay proxies, and tunnels for local testing");
        System.out.println();
        System.out.println("Usage: mock-server [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -f <file>          Start server with the given config file");
        System.out.println("  -g <type>          Generate a template config to stdout");
        System.out.println("  -g <type> -o <file> Generate a template config to a file");
        System.out.println("  -l                 List available config types");
        System.out.println("  -h                 Show this help message");
        System.out.println();
        printAvailableTypes();
    }

    /**
     * Prints available types to stdout.
     */
    public void printAvailableTypes() {
        System.out.println("Available config types:");
        TEMPLATE_DESCRIPTIONS.forEach((type, desc) -> System.out.printf("  %-20s %s%n", type, desc));
    }
}
