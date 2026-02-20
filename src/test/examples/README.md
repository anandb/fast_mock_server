# Mock Server Examples

This directory contains example configurations for the mock server. These examples demonstrate various features and capabilities of the server.

## Available Configuration Files

The following configuration files are available in this directory:

### 1. [server-config-test.jsonmc](./server-config-test.jsonmc)
Basic test server configuration demonstrating a simple API endpoint.
- **Port:** 9001
- **Features:** Simple GET endpoint with global headers.

### 2. [server-config-example.jsonmc](./server-config-example.jsonmc)
Multi-server configuration with both HTTP and HTTPS servers.
- **Servers:** api-server (HTTP on 9001), secure-server (HTTPS on 8443).
- **Features:** TLS/SSL configuration, global headers.

### 3. [server-config-basicauth-example.jsonmc](./server-config-basicauth-example.jsonmc)
Server with basic authentication.
- **Port:** 9090
- **Features:** Basic authentication (username: `admin`, password: `secret123`).

### 4. [server-config-mtls-example.jsonmc](./server-config-mtls-example.jsonmc)
Server with mutual TLS (mTLS) authentication.
- **Port:** 8443 (HTTPS)
- **Features:** Client certificate verification, embedded certificates.

### 5. [server-config-pathvars-example.jsonmc](./server-config-pathvars-example.jsonmc)
Configuration demonstrating path variable support in Freemarker templates.
- **Port:** 9001
- **Features:** Dynamic response generation using path variables.

### 6. [server-config-sse-example.jsonmc](./server-config-sse-example.jsonmc)
Server demonstrating Server-Sent Events (SSE).
- **Port:** 9002
- **Features:** Multiple SSE streams with configurable intervals.

### 7. [server-config-relay-example.jsonmc](./server-config-relay-example.jsonmc)
Relay server that forwards requests to a remote API with OAuth2 authentication.
- **Port:** 8090
- **Features:** OAuth2 token acquisition and request forwarding.

### 8. [server-config-relay-no-auth-example.jsonmc](./server-config-relay-no-auth-example.jsonmc)
Relay server that forwards requests to a remote API without authentication.
- **Port:** 8090
- **Features:** Simple proxying and custom header injection.

### 9. [server-config-files-example.jsonmc](./server-config-files-example.jsonmc)
Configuration demonstrating file downloads and multi-part responses.
- **Port:** 8082
- **Features:** Multi-part file downloads.

### 10. [server-config-tunnel-example.jsonmc](./server-config-tunnel-example.jsonmc)
Relay server that forwards requests to a Kubernetes pod via kubectl port-forward tunnel.
- **Port:** 9001
- **Features:** Kubernetes pod discovery and tunnel lifecycle management.

## Usage

To run the mock server with a specific configuration file, use the following command from the project root:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmock.server.config.file=./src/test/examples/<config-file-name>.jsonmc"
```

Replace `<config-file-name>` with the name of the desired configuration file (e.g., `server-config-test`).

### Example: Running the Test API
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmock.server.config.file=./src/test/examples/server-config-test.jsonmc"
```

## Directory Structure
All configuration files are located directly in this directory: `src/test/examples/`.
There are no sub-directories or additional shell scripts.
