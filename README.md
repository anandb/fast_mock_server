# MockServer Manager

A Spring Boot application for managing multiple MockServer instances with support for TLS/mTLS and global response headers.

## Features

- **Multiple MockServer Instances**: Create and manage multiple mock servers in a single process
- **File-based Configuration**: Configure servers via JSON/JSONMC configuration file at startup
- **Relay/Proxy Mode**: Forward requests to remote servers with optional OAuth2 authentication
- **Kubernetes Tunnel Support**: Establish kubectl port-forward tunnels to Kubernetes pods
- **TLS/HTTPS Support**: Enable HTTPS with custom certificates
- **Mutual TLS (mTLS)**: Client certificate validation with CA certificate
- **Basic Authentication**: HTTP Basic Auth protection for mock servers
- **Global Headers**: Apply headers to all responses from a server
- **Header Merging**: Intelligent merge of global and expectation-specific headers

## Request Handling Flow

The following call tree illustrates how a typical HTTP request is processed by the MockServer Manager when an enhanced expectation is matched:

```text
Incoming HTTP Request (MockServer Port)
│
└── EnhancedResponseCallback.handle(HttpRequest)
    │
    ├── Extract context (path variables, cookies, params)
    │
    ├── Find matching Strategy (supports(config) == true)
    │   │
    │   ├── StaticResponseStrategy.handle(...)
    │   │   └── Return pre-configured HttpResponse
    │   │
    │   ├── DynamicFileStrategy.handle(...)
    │   │   ├── evaluateFilePathTemplate()
    │   │   ├── findFirstFileWithPrefix() (Glob search)
    │   │   └── Process via FreemarkerTemplateService (if template)
    │   │
    │   ├── SSEResponseStrategy.handle(...)
    │   │   └── Format messages into text/event-stream
    │   │
    │   └── RelayResponseStrategy.handle(...)
    │       └── RelayService.relayRequest()
    │           └── OAuth2TokenService.getAccessToken() (if configured)
    │
    ├── mergeGlobalHeaders(response)
    │   └── Apply server-level global headers
    │
    └── Return final HttpResponse to Client
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│       Spring Boot Management App (Control Port 8080)    │
├─────────────────────────────────────────────────────────┤
│  ConfigurationLoaderService                             │
│  - Loads config from file                               │
│  - Creates servers on startup                           │
├─────────────────────────────────────────────────────────┤
│  MockServerManager    │  EnhancedResponseCallback       │
│  - Server registry    │  - Strategy Router             │
│  - Lifecycle mgmt     │  - Global Header Merging      │
├─────────────────────────────────────────────────────────┤
│                  Response Strategies                    │
│  - Static  - Dynamic File  - SSE  - Relay             │
│  - Kubernetes Tunnel                                  │
└─────────────────────────────────────────────────────────┘
             │                          │
             ▼                          ▼
      ┌─────────────┐          ┌─────────────┐
      │ MockServer  │          │ MockServer  │
      │ Port 1080   │          │ Port 1443   │
      │ (HTTP)      │          │ (HTTPS)     │
      └─────────────┘          └─────────────┘
```
┌─────────────────────────────────────────────────────────┐
│       Spring Boot Management App (Control Port 8080)    │
├─────────────────────────────────────────────────────────┤
│  ConfigurationLoaderService                             │
│  - Loads config from file or base64                    │
│  - Creates servers on startup                          │
├─────────────────────────────────────────────────────────┤
│  MockServerManager    │  EnhancedResponseCallback       │
│  - Server registry    │  - Strategy Router             │
│  - Lifecycle mgmt     │  - Global Header Merging      │
│  - Tunnel management │                                │
├─────────────────────────────────────────────────────────┤
│                  Response Strategies                    │
│  - Static  - Dynamic File  - SSE  - Relay             │
│  - Kubernetes Tunnel                                  │
└─────────────────────────────────────────────────────────┘
            │                          │
            ▼                          ▼
     ┌─────────────┐          ┌─────────────┐
     │ MockServer  │          │ MockServer  │
     │ Port 1080   │          │ Port 1443   │
     │ (HTTP)      │          │ (HTTPS)     │
     └─────────────┘          └─────────────┘
```

## Prerequisites

- Java 17 or higher
- Maven 3.8+

## Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR
java -jar target/mock-server-1.0.0.jar

# Run with configuration file
java -Dmock.server.config.file=server-config.json -jar target/mock-server-1.0.0.jar

# Or with Maven
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmock.server.config.file=server-config.json"
```

The management application runs on `http://localhost:8080`. All server configuration is done via file-based configuration at startup.

## Loading Configuration from File

You can automatically create servers and configure expectations at startup by providing a JSON configuration file. This is useful for:
- Consistent test environments
- CI/CD pipelines
- Automated testing setups
- Quick server setup without API calls
- Docker containers

### Configuration File Format

Create a JSON file (e.g., `server-config.json`) with an array of server configurations:

```json
[
  {
    "server": {
      "serverId": "api-server",
      "port": 8081,
      "description": "API Mock Server",
      "globalHeaders": [
        {
          "name": "X-API-Version",
          "value": "1.0"
        }
      ]
    },
    "expectations": [
      {
        "httpRequest": {
          "method": "GET",
          "path": "/api/users"
        },
        "httpResponse": {
          "statusCode": 200,
          "body": "{\"users\": [{\"id\": 1, \"name\": \"John Doe\"}]}"
        }
      }
    ]
  }
]
```

### Usage

Specify the configuration file path using the `mock.server.config.file` system property:

```bash
# Using java command
java -Dmock.server.config.file=/path/to/server-config.json -jar target/mock-server-1.0.0.jar

# Using Maven
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmock.server.config.file=./server-config.json"

# Using relative path
java -Dmock.server.config.file=./config/servers.json -jar target/mock-server-1.0.0.jar
```

### Docker Usage

When running inside a Docker container, place your configuration file at `/server.jsonmc` in the container filesystem. The application will automatically detect and load it:

```bash
# Build the JAR
mvn clean package -DskipTests

# Build Docker image with config
docker build -t mock-server .
# (Ensure server.jsonmc is copied to root of container)

# Run the container - config at /server.jsonmc will be auto-loaded
# Note: Use --net=host due to dynamic port mappings for mock servers
docker run --net=host mock-server
```

**Important:** Due to the complexity of managing multiple dynamic ports for mock servers (each server can use different ports), you must run the container with `--net=host`. This allows the mock servers to bind directly to host ports without Docker port mapping overhead.

**Format Support:** Both JSON and JSONMC formats are supported. The system auto-detects JSONMC format by looking for comment markers (`//` or `/*`) in the file content.

**Note:** If no configuration file is specified and no `/server.jsonmc` exists, the application starts without pre-configured servers.

## File Glob Pattern Support

The application supports file glob patterns for flexible file matching in both configuration loading and file response callbacks. This feature allows you to specify file path prefixes instead of exact file paths, making it easier to work with dynamic file names or when the exact file name is not known in advance.

### Configuration File Loading with Glob Patterns

When loading server configurations at startup, you can specify a file path prefix using the `mock.server.config.file` system property. The system will search for the first file that starts with the specified prefix:

```bash
# Instead of exact file path
java -Dmock.server.config.file=/path/to/server-config.json -jar target/mock-server-1.0.0.jar

# You can use a prefix - finds first file starting with "server-config"
java -Dmock.server.config.file=/path/to/server-config -jar target/mock-server-1.0.0.jar
```

**Examples:**
- Prefix: `server-config` → matches `server-config.json`, `server-config-example.json`, `server-config-v2.json`, etc.
- Prefix: `config/servers` → matches `config/servers.json`, `config/servers-prod.json`, etc.
- Prefix: `test-config` → matches `test-config.jsonmc`, `test-config-backup.json`, etc.

The search is performed recursively from the current directory, and the first matching file is used.

### File Response Callbacks with Glob Patterns

When configuring expectations with file responses, you can use glob patterns to specify file path prefixes. The system will find the first file that starts with the specified prefix:

```json
{
  "httpRequest": {
    "method": "GET",
    "path": "/api/download/report"
  },
  "httpResponse": {
    "statusCode": 200,
    "file": "/path/to/documents/report"
  }
}
```

**Examples:**
- Prefix: `/path/to/documents/report` → matches `report.pdf`, `report-v2.pdf`, `report-final.pdf`, etc.
- Prefix: `/data/exports/user-data` → matches `user-data.csv`, `user-data-2024.csv`, `user-data-backup.json`, etc.
- Prefix: `/tmp/cache/api-response` → matches `api-response.json`, `api-response.xml`, etc.

### Benefits of File Glob Patterns

- **Flexible File Matching**: No need to know exact file names in advance
- **Dynamic File Selection**: Works well with generated or timestamped files
- **Simplified Configuration**: Reduces the need for exact file path specifications
- **Environment Agnostic**: Easier to work across different environments where file names might vary

### Important Notes

- Glob pattern matching searches recursively from the current working directory
- Only the **first matching file** is used (alphabetically by file path)
- If no matching file is found, the system returns a 404 error
- Glob patterns work with both regular files and FreeMarker template expressions
- The search includes all subdirectories and is case-sensitive on most filesystems

### Configuration File Structure

Each server configuration object contains:
- **server** (required): Server creation parameters
  - `serverId`: Unique identifier
  - `port`: Port number (1024-65535)
  - `description`: Optional description
  - `tlsConfig`: Optional TLS/HTTPS configuration
  - `globalHeaders`: Optional global response headers
  - `relays`: Optional relay/tunnel configuration
- **expectations** (optional): Array of expectations to configure on this server

**Note:** This application does not expose a REST API for server management. All configuration is loaded from the configuration file at startup.

See `server-config-example.json` for a complete example with multiple servers and various configurations.

### Benefits

- **Reproducible Environments**: Same configuration across all environments
- **Version Control**: Track configuration changes in Git
- **Quick Setup**: Start multiple servers with expectations in one command
- **CI/CD Integration**: Easily integrate with automated testing pipelines
- **No API Calls**: Servers and expectations ready immediately on startup

## Quick Setup

To create and configure mock servers, use the file-based configuration:

```bash
# Run with a configuration file
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmock.server.config.file=./server-config.jsonmc"
```

See `server-config-example.json` for a complete configuration example with multiple servers.

## Header Merging Behavior

When a server has global headers configured, they are automatically merged with expectation-specific headers:

**Global Headers** (configured at server creation):
```json
{
  "globalHeaders": [
    {"name": "X-Service-Version", "value": "1.0.0"},
    {"name": "X-Environment", "value": "test"}
  ]
}
```

**Expectation Headers**:
```json
{
  "headers": {
    "Content-Type": ["application/json"],
    "X-Service-Version": ["2.0.0"]
  }
}
```

**Final Response Headers** (expectation headers override global):
- `Content-Type: application/json` (from expectation)
- `X-Service-Version: 2.0.0` (from expectation - overrides global)
- `X-Environment: test` (from global)

## Complete Usage Example

Create a configuration file `server-config.jsonmc`:

```json
[
  {
    "server": {
      "serverId": "user-service",
      "port": 1080,
      "description": "User Service Mock",
      "globalHeaders": [
        {"name": "X-Service-Name", "value": "user-service"},
        {"name": "X-Mock-Server", "value": "true"}
      ]
    },
    "expectations": [
      {
        "httpRequest": {
          "method": "GET",
          "path": "/users/123"
        },
        "httpResponse": {
          "statusCode": 200,
          "headers": {
            "Content-Type": ["application/json"]
          },
          "body": {
            "id": 123,
            "name": "John Doe"
          }
        }
      }
    ]
  }
]
```

Start the server:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmock.server.config.file=./server-config.jsonmc"
```

Test the mock:

```bash
curl http://localhost:1080/users/123
```

**Response**:
```json
{
  "id": 123,
  "name": "John Doe"
}
```

**Response Headers**:
```
Content-Type: application/json
X-Service-Name: user-service
X-Mock-Server: true
```

## TLS/HTTPS Example

### Generate Self-Signed Certificate (for testing)

```bash
# Generate private key
openssl genrsa -out server.key 2048

# Generate certificate
openssl req -new -x509 -key server.key -out server.crt -days 365 \
  -subj "/CN=localhost"

# View certificate content
cat server.crt
cat server.key
```

### Create HTTPS Server

Use a configuration file:

```json
{
  "server": {
    "serverId": "https-service",
    "port": 1443,
    "tlsConfig": {
      "certificate": "-----BEGIN CERTIFICATE-----\nMIID...\n-----END CERTIFICATE-----",
      "privateKey": "-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----"
    }
  }
}
```

### Test HTTPS Server

```bash
curl -k https://localhost:1443/test
```

## mTLS Example

### Generate CA and Client Certificates

```bash
# Create CA
openssl genrsa -out ca.key 2048
openssl req -new -x509 -key ca.key -out ca.crt -days 365 \
  -subj "/CN=Test CA"

# Create server cert signed by CA
openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr -subj "/CN=localhost"
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out server.crt -days 365

# Create client cert signed by CA
openssl genrsa -out client.key 2048
openssl req -new -key client.key -out client.csr -subj "/CN=client"
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out client.crt -days 365
```

### Create mTLS Server

Use a configuration file:

```json
{
  "server": {
    "serverId": "mtls-service",
    "port": 1444,
    "tlsConfig": {
      "certificate": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
      "privateKey": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----",
      "mtlsConfig": {
        "caCertificate": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
        "requireClientAuth": true
      }
    }
  }
}
```

Test with Client Certificate:

```bash
curl -k https://localhost:1444/test \
  --cert client.crt \
  --key client.key
```

## Relay Server (Proxy Mode)

The relay server feature allows you to create a mock server that acts as a proxy, forwarding all incoming requests to a remote server. This is useful for testing against real APIs, adding authentication layers, or creating simple proxy servers.

### Key Features

- **Optional OAuth2 Authentication**: Automatically obtains and manages OAuth2 access tokens
- **Token Caching**: Caches tokens to minimize token endpoint calls
- **Custom Headers**: Add custom headers to all relayed requests
- **No Authentication Mode**: Relay requests without any authentication
- **Full Request Forwarding**: Forwards method, path, query params, headers, and body
- **Transparent Response**: Returns exact response from remote server

### Configuration Examples

**Simple relay without authentication:**
```json
{
  "server": {
    "serverId": "simple-relay",
    "port": 8090,
    "relays": [{
      "remoteUrl": "https://api.example.com"
    }]
  }
}
```

**Relay with OAuth2:**
```json
{
  "server": {
    "serverId": "oauth-relay",
    "port": 8090,
    "relays": [{
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "client-id",
      "clientSecret": "client-secret"
    }]
  }
}
```

**Relay with custom headers:**
```json
{
  "server": {
    "serverId": "custom-relay",
    "port": 8090,
    "relays": [{
      "remoteUrl": "https://api.example.com",
      "headers": {
        "X-API-Version": "v2",
        "X-Client-App": "test-suite"
      }
    }]
  }
}
```

### Testing the Relay Server

Once the server is started with the configuration file, any request to the relay server is automatically forwarded to the remote server:

```bash
# Request to relay server
curl http://localhost:8090/users/123

# This is forwarded to: https://api.example.com/users/123
# (with OAuth2 token if configured)
```

### Relay Configuration Parameters

**Required:**
- `remoteUrl`: Base URL of the remote server

**OAuth2 Authentication (all three required if using OAuth2):**
- `tokenUrl`: OAuth2 token endpoint
- `clientId`: OAuth2 client ID
- `clientSecret`: OAuth2 client secret

**Optional:**
- `prefixes`: List of path prefixes to match (default: `["/**"]`)
- `scope`: OAuth2 scope to request
- `grantType`: OAuth2 grant type (default: "client_credentials")
- `headers`: Custom headers to add to all relayed requests

### Important Notes

- When relay is enabled, **expectations are ignored** - all requests are forwarded
- Multiple relays can be configured with different prefixes. The **longest matching prefix** is selected
- OAuth2 is completely optional - omit token parameters for no authentication
- If providing OAuth2 config, all three parameters (`tokenUrl`, `clientId`, `clientSecret`) must be provided
- Tokens are automatically cached and refreshed when expired

See `examples/server-config-relay-example.jsonmc` and `examples/server-config-relay-no-auth-example.jsonmc` for complete examples.

For detailed relay configuration documentation, see [docs/RELAY_CONFIGURATION.md](docs/RELAY_CONFIGURATION.md).

### Kubernetes Tunnel Support

The relay feature also supports establishing kubectl port-forward tunnels to Kubernetes pods. This enables access to services running inside a Kubernetes cluster without exposing them publicly.

**Key Features:**
- **Automatic Pod Discovery**: Finds pods using prefix filter
- **Auto-assigned Ports**: Host port automatically assigned from range 9000-11000
- **Sequential Creation**: Multiple tunnels created one at a time
- **Graceful Shutdown**: Tunnels automatically killed on server stop

**Example Configuration:**
```json
{
  "server": {
    "serverId": "k8s-tunnel",
    "port": 9001,
    "relays": [{
      "tunnelConfig": {
        "namespace": "production",
        "podPrefix": "api-service-",
        "podPort": 8080
      },
      "prefixes": ["/api/**"]
    }]
  }
}
```

For detailed tunnel configuration documentation, see [docs/RELAY_CONFIGURATION.md](docs/RELAY_CONFIGURATION.md).

## Error Handling

All errors return a consistent format:

```json
{
  "errorCode": "SERVER_NOT_FOUND",
  "message": "Server not found with ID: unknown-server",
  "timestamp": "2025-10-02T21:30:00"
}
```

**Error Codes**:
- `SERVER_NOT_FOUND` (404) - Server ID not found
- `SERVER_ALREADY_EXISTS` (409) - Server ID already in use
- `INVALID_CERTIFICATE` (400) - Certificate validation failed
- `SERVER_CREATION_FAILED` (500) - Server creation error
- `INVALID_EXPECTATION` (400) - Invalid expectation JSON
- `VALIDATION_FAILED` (400) - Request validation errors

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Management API port
server.port=8080

# Certificate temp directory
mockserver.cert.temp-dir=/tmp/mockserver-certs

# Auto-cleanup certificates on shutdown
mockserver.cert.cleanup-on-shutdown=true

# Logging level
logging.level.io.github.anandb.mockserver=DEBUG
```

## Dependencies

- Spring Boot 3.5.3
- MockServer Netty (no-dependencies) 5.15.0
- Lombok
- Jackson (JSON processing)
- FreeMarker (Templating engine)

## License

This project is provided as-is for demonstration purposes.

## Support

For issues or questions, please refer to:
- [MockServer Documentation](https://www.mock-server.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
