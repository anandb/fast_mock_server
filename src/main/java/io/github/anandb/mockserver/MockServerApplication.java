package io.github.anandb.mockserver;

import io.github.anandb.mockserver.service.ConfigGeneratorService;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.apache.commons.lang3.StringUtils.isBlank;

@SpringBootApplication
public class MockServerApplication {

    static {
        try {
            // Extract logging.properties to a temp file for JUL to use
            java.nio.file.Path tempLogConfig = java.nio.file.Files.createTempFile("logging", ".properties");
            try (var is = MockServerApplication.class.getResourceAsStream("/logging.properties");
                 var os = java.nio.file.Files.newOutputStream(tempLogConfig)) {
                if (is != null) {
                    is.transferTo(os);
                    System.setProperty("java.util.logging.config.file", tempLogConfig.toAbsolutePath().toString());
                    java.util.logging.LogManager.getLogManager().readConfiguration();
                }
            }

            // Bridge JUL to SLF4J
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();

            // Tell MockServer to stay quiet and avoid manual JUL config
            if (isBlank(System.getProperty("mockserver.logLevel", ""))) {
                System.setProperty("mockserver.logLevel", "WARN");
            }
            
        } catch (Exception ignored) {
            // Silently ignore if configuration fails
        }
    }

    public static void main(String[] args) {
        // If running as a container, see if a configuration has been copied/mounted
        File file = new File("/.dockerenv");
        if (file.exists() && isBlank(System.getProperty("mock.server.config.file", ""))) {
            System.setProperty("mock.server.config.file", "/server.jsonmc");
        }

        // No args → print usage and exit
        if (args.length == 0) {
            printUsage();
            System.exit(0);
            return;
        }

        // -h → print usage and exit
        if (hasArg(args, "-h", "--help")) {
            printUsage();
            System.exit(0);
            return;
        }

        // -l → list types and exit
        if (hasArg(args, "-l", "--list")) {
            new ConfigGeneratorService().printAvailableTypes();
            System.exit(0);
            return;
        }

        // -g <type> [-o <file>] → generate template and exit
        String type = getOptionValue(args, "-g", "--generate");
        if (type != null) {
            String outputPath = getOptionValue(args, "-o", "--output");
            ConfigGeneratorService generator = new ConfigGeneratorService();
            try {
                if (outputPath != null) {
                    generator.generate(type, outputPath);
                    System.out.println("Generated " + type + " config → " + outputPath);
                } else {
                    System.out.print(generator.loadTemplate(type));
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Error: " + e.getMessage());
                generator.printAvailableTypes();
                System.exit(1);
                return;
            } catch (Exception e) {
                System.err.println("Failed: " + e.getMessage());
                System.exit(1);
                return;
            }
            System.exit(0);
            return;
        }

        // -f <file> → set config file property before Spring starts
        // (must happen before ConfigurationLoaderService.@PostConstruct)
        String configFile = getOptionValue(args, "-f", "--config");
        if (configFile != null) {
            System.setProperty("mock.server.config.file", configFile);
        }

        // Start the server
        SpringApplication.run(MockServerApplication.class, args);
    }

    private static String getOptionValue(String[] args, String shortFlag, String longFlag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (shortFlag.equals(args[i]) || longFlag.equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }

    private static boolean hasArg(String[] args, String... flags) {
        for (String arg : args) {
            for (String flag : flags) {
                if (flag.equals(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void printUsage() {
        System.out.println("Usage: mock-server [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -f, --config <file>          Start server with the given config file");
        System.out.println("  -g, --generate <type>        Generate a template config to stdout");
        System.out.println("  -g, --generate <type> -o, --output <file>  Generate to a file");
        System.out.println("  -l, --list                   List available config types");
        System.out.println("  -h, --help                   Show this help message");
        System.out.println();
        printTypes();
    }

    private static void printTypes() {
        String[][] types = {
            {"simple", "Basic API mock server"},
            {"multi", "HTTP + HTTPS multi-server with TLS"},
            {"basicauth", "Basic authentication (username/password)"},
            {"mtls", "Mutual TLS (mTLS) with client certs"},
            {"pathvars", "Path variables and FreeMarker templates"},
            {"sse", "Server-Sent Events (SSE) streaming"},
            {"relay", "Relay proxy with OAuth2 authentication"},
            {"relay-no-auth", "Relay proxy without authentication"},
            {"files", "File download server"},
            {"tunnel", "Kubernetes pod tunnel relay"},
        };
        System.out.println("Available config types:");
        for (String[] t : types) {
            System.out.printf("  %-20s %s%n", t[0], t[1]);
        }
    }
}
