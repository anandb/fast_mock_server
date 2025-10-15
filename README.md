# MockServer Manager

A Spring Boot application for managing multiple MockServer instances with support for TLS/mTLS and global response headers.

## Features

- **Multiple MockServer Instances**: Create and manage multiple mock servers in a single process
- **Dynamic Configuration**: Configure expectations via REST API
- **Relay/Proxy Mode**: Forward requests to remote servers with optional OAuth2 authentication
- **TLS/HTTPS Support**: Enable HTTPS with custom certificates
- **Mutual TLS (mTLS)**: Client certificate validation with CA certificate
- **Basic Authentication**: HTTP Basic Auth protection for mock servers
- **Global Headers**: Apply headers to all responses from a server
- **Header Merging**: Intelligent merge of global and expectation-specific headers

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│          Spring Boot Management App (Port 8080)        │
├─────────────────────────────────────────────────────────┤
│  ServerController     │  ExpectationController          │
│  - Create servers     │  - Configure expectations       │
│  - List servers       │  - Merge global headers         │
│  - Delete servers     │  - Clear expectations           │
├─────────────────────────────────────────────────────────┤
│  MockServerManager    │  TlsConfigurationService        │
│  - Server registry    │  - Certificate validation       │
│  - Lifecycle mgmt     │  - Temp file management         │
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

The management API will be available at `http://localhost:8080`

## Loading Configuration from File or Base64

You can automatically create servers and configure expectations at startup by providing either a JSON configuration file or base64-encoded configuration content. This is useful for:
- Consistent test environments
- CI/CD pipelines
- Automated testing setups
- Quick server setup without API calls
- Passing configuration via environment variables or command line

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

You have two options for loading configuration at startup:

#### Option 1: Configuration File

Specify the configuration file path using the `mock.server.config.file` system property:

```bash
# Using java command
java -Dmock.server.config.file=/path/to/server-config.json -jar target/mock-server-1.0.0.jar

# Using Maven
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmock.server.config.file=./server-config.json"

# Using relative path
java -Dmock.server.config.file=./config/servers.json -jar target/mock-server-1.0.0.jar
```

#### Option 2: Base64-Encoded Configuration

Alternatively, provide base64-encoded configuration content using the `mock.server.config.fileb64` system property. This is useful when:
- Configuration needs to be passed via environment variables
- Files cannot be easily mounted (e.g., container environments)
- Configuration is generated dynamically
- Avoiding filesystem access is preferred

```bash
# First, encode your configuration file to base64
CONFIG_B64=$(base64 -w 0 server-config.json)

# Then pass it as a system property
java -Dmock.server.config.fileb64="$CONFIG_B64" -jar target/mock-server-1.0.0.jar

# Or with Maven
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmock.server.config.fileb64=$CONFIG_B64"

# Direct inline base64 (for short configs)
java -Dmock.server.config.fileb64="WwogIHsKICAgICJzZXJ2ZXIiOiB7CiAgICAgICJzZXJ2ZXJJZCI6ICJ0ZXN0IiwKICAgICAgInBvcnQiOiA4MDgxCiAgICB9CiAgfQpd" -jar target/mock-server-1.0.0.jar
```

**Priority:** The `mock.server.config.fileb64` property is checked first. If it's set, the application loads configuration from the base64-encoded content and ignores `mock.server.config.file`. If neither property is set, the application starts without pre-configured servers.

**Format Support:** Both JSON and JSONMC formats are supported. The system auto-detects JSONMC format by looking for comment markers (`//` or `/*`) in the decoded content.

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
- **server** (required): Server creation parameters (same as POST /api/servers)
  - `serverId`: Unique identifier
  - `port`: Port number (1024-65535)
  - `description`: Optional description
  - `tlsConfig`: Optional TLS/HTTPS configuration
  - `globalHeaders`: Optional global response headers
- **expectations** (optional): Array of expectations to configure on this server

See `server-config-example.json` for a complete example with multiple servers and various configurations.

### Benefits

- **Reproducible Environments**: Same configuration across all environments
- **Version Control**: Track configuration changes in Git
- **Quick Setup**: Start multiple servers with expectations in one command
- **CI/CD Integration**: Easily integrate with automated testing pipelines
- **No API Calls**: Servers and expectations ready immediately on startup

## API Documentation

### Server Management

#### Create a Server

**Endpoint**: `POST /api/servers`

**Simple HTTP Server**:
```json
{
  "serverId": "simple-service",
  "port": 1080,
  "description": "Simple HTTP mock server"
}
```

**HTTPS Server with Global Headers**:
```json
{
  "serverId": "secure-api",
  "port": 1443,
  "description": "Secure API with global headers",
  "tlsConfig": {
    "certificate": "-----BEGIN CERTIFICATE-----\nMIID...\n-----END CERTIFICATE-----",
    "privateKey": "-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----"
  },
  "globalHeaders": [
    {"name": "X-Service-Version", "value": "1.0.0"},
    {"name": "X-Environment", "value": "test"},
    {"name": "Access-Control-Allow-Origin", "value": "*"}
  ]
}
```

**HTTPS Server with mTLS**:
```json
{
  "serverId": "mtls-service",
  "port": 1444,
  "description": "Service with mutual TLS",
  "tlsConfig": {
    "certificate": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
    "privateKey": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----",
    "mtlsConfig": {
      "caCertificate": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
      "requireClientAuth": true
    }
  }
}
```

**Server with Basic Authentication**:
```json
{
  "serverId": "protected-api",
  "port": 9090,
  "description": "API server with basic authentication",
  "basicAuthConfig": {
    "username": "admin",
    "password": "secret123"
  },
  "globalHeaders": [
    {"name": "X-API-Version", "value": "1.0"}
  ]
}
```

**Response**: `201 Created`
```json
{
  "serverId": "secure-api",
  "port": 1443,
  "description": "Secure API with global headers",
  "protocol": "https",
  "baseUrl": "https://localhost:1443",
  "tlsEnabled": true,
  "mtlsEnabled": false,
  "basicAuthEnabled": false,
  "globalHeaders": [
    {"name": "X-Service-Version", "value": "1.0.0"},
    {"name": "X-Environment", "value": "test"}
  ],
  "createdAt": "2025-10-02T21:30:00",
  "status": "running"
}
```

#### List All Servers

**Endpoint**: `GET /api/servers`

**Response**: `200 OK`
```json
[
  {
    "serverId": "simple-service",
    "port": 1080,
    "protocol": "http",
    "baseUrl": "http://localhost:1080",
    "status": "running"
  },
  {
    "serverId": "secure-api",
    "port": 1443,
    "protocol": "https",
    "baseUrl": "https://localhost:1443",
    "status": "running"
  }
]
```

#### Get Server Details

**Endpoint**: `GET /api/servers/{serverId}`

**Response**: `200 OK` - Returns ServerInfo object

#### Delete a Server

**Endpoint**: `DELETE /api/servers/{serverId}`

**Response**: `204 No Content`

### Expectation Management

#### Configure Expectations

**Endpoint**: `POST /api/servers/{serverId}/expectations`

**Single Expectation**:
```json
{
  "httpRequest": {
    "method": "GET",
    "path": "/api/users/123"
  },
  "httpResponse": {
    "statusCode": 200,
    "headers": {
      "Content-Type": ["application/json"]
    },
    "body": {
      "id": 123,
      "name": "John Doe",
      "email": "john@example.com"
    }
  }
}
```

**Multiple Expectations (Array)**:
```json
[
  {
    "httpRequest": {
      "method": "GET",
      "path": "/api/users"
    },
    "httpResponse": {
      "statusCode": 200,
      "headers": {
        "Content-Type": ["application/json"]
      },
      "body": {
        "users": [
          {"id": 1, "name": "Alice"},
          {"id": 2, "name": "Bob"}
        ]
      }
    }
  },
  {
    "httpRequest": {
      "method": "POST",
      "path": "/api/users"
    },
    "httpResponse": {
      "statusCode": 201,
      "headers": {
        "Content-Type": ["application/json"],
        "Location": ["/api/users/3"]
      },
      "body": {
        "id": 3,
        "name": "Charlie"
      }
    }
  }
]
```

**Response**: `200 OK`
```
Successfully configured 2 expectation(s) for server: simple-service
```

**Multi-Part File Download Expectations**:

For serving file downloads, use the `files` field in `httpResponse` instead of `body`:

```json
{
  "httpRequest": {
    "method": "GET",
    "path": "/api/download/documents"
  },
  "httpResponse": {
    "statusCode": 200,
    "file": "/absolute/path/to/document1.pdf",
    "headers": {
      "X-Download-Type": ["multi-part"]
    }
  }
}
```

The `files` field:
- Accepts an array of absolute file paths
- Automatically serves files as multipart/form-data response
- Detects content types based on file extensions
- Works with any file type (PDF, images, CSV, ZIP, etc.)
- When `files` is present, the `body` field should be omitted

Supported file types (auto-detected):
- Documents: PDF, TXT, CSV
- Archives: ZIP
- Images: PNG, JPG, JPEG, GIF
- Data: JSON, XML
- Office: XLSX (served as application/octet-stream)
- Default: application/octet-stream for unknown types

**Example with Single File**:
```json
{
  "httpRequest": {
    "method": "GET",
    "path": "/api/download/report"
  },
  "httpResponse": {
    "statusCode": 200,
    "file": "/path/to/report.pdf"
  }
}
```

**Example with Mixed File Types**:
```json
{
  "httpRequest": {
    "method": "POST",
    "path": "/api/export/data"
  },
  "httpResponse": {
    "statusCode": 200,
    "file":  "/path/to/data.csv"
  }
}
```

See `examples/server-config-files-example.jsonmc` for complete examples.

#### Get Current Expectations

**Endpoint**: `GET /api/servers/{serverId}/expectations`

**Response**: `200 OK` - Returns array of active expectations

#### Clear All Expectations

**Endpoint**: `DELETE /api/servers/{serverId}/expectations`

**Response**: `200 OK`

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

### 1. Create a Mock Server

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "user-service",
    "port": 1080,
    "description": "User Service Mock",
    "globalHeaders": [
      {"name": "X-Service-Name", "value": "user-service"},
      {"name": "X-Mock-Server", "value": "true"}
    ]
  }'
```

### 2. Configure Expectations

```bash
curl -X POST http://localhost:8080/api/servers/user-service/expectations \
  -H "Content-Type: application/json" \
  -d '{
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
  }'
```

### 3. Test the Mock

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

### 4. List All Servers

```bash
curl http://localhost:8080/api/servers
```

### 5. Delete Server

```bash
curl -X DELETE http://localhost:8080/api/servers/user-service
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

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "https-service",
    "port": 1443,
    "tlsConfig": {
      "certificate": "'"$(cat server.crt | sed 's/$/\\n/' | tr -d '\n')"'",
      "privateKey": "'"$(cat server.key | sed 's/$/\\n/' | tr -d '\n')"'"
    }
  }'
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

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "mtls-service",
    "port": 1444,
    "tlsConfig": {
      "certificate": "'"$(cat server.crt | sed 's/$/\\n/' | tr -d '\n')"'",
      "privateKey": "'"$(cat server.key | sed 's/$/\\n/' | tr -d '\n')"'",
      "mtlsConfig": {
        "caCertificate": "'"$(cat ca.crt | sed 's/$/\\n/' | tr -d '\n')"'",
        "requireClientAuth": true
      }
    }
  }'
```

### Test with Client Certificate

```bash
curl -k https://localhost:1444/test \
  --cert client.crt \
  --key client.key
```

## Basic Authentication Example

Basic authentication adds HTTP Basic Auth protection to your mock server. When enabled, all requests must include valid credentials.

### Create Server with Basic Auth

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "auth-api",
    "port": 9090,
    "description": "Protected API",
    "basicAuthConfig": {
      "username": "admin",
      "password": "secret123"
    }
  }'
```

### Configure Expectations

Expectations are configured normally - the basic auth is applied automatically:

```bash
curl -X POST http://localhost:8080/api/servers/auth-api/expectations \
  -H "Content-Type: application/json" \
  -d '{
    "httpRequest": {
      "method": "GET",
      "path": "/api/data"
    },
    "httpResponse": {
      "statusCode": 200,
      "body": {"message": "Protected data"}
    }
  }'
```

### Test with Credentials

```bash
# Without credentials - will fail
curl http://localhost:9090/api/data

# With correct credentials - will succeed
curl -u admin:secret123 http://localhost:9090/api/data

# Or using Authorization header
curl -H "Authorization: Basic YWRtaW46c2VjcmV0MTIz" http://localhost:9090/api/data
```

### Combining Basic Auth with TLS

You can enable both basic authentication and TLS for extra security:

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "secure-auth-api",
    "port": 9443,
    "description": "HTTPS API with Basic Auth",
    "tlsConfig": {
      "certificate": "'"$(cat server.crt | sed 's/$/\\n/' | tr -d '\n')"'",
      "privateKey": "'"$(cat server.key | sed 's/$/\\n/' | tr -d '\n')"'"
    },
    "basicAuthConfig": {
      "username": "admin",
      "password": "secret123"
    }
  }'
```

Test the secured endpoint:
```bash
curl -k -u admin:secret123 https://localhost:9443/api/data
```

### Configuration File with Basic Auth

See `server-config-basicauth-example.json` for a complete example configuration file that includes basic authentication.

## Relay Server (Proxy Mode)

The relay server feature allows you to create a mock server that acts as a proxy, forwarding all incoming requests to a remote server. This is useful for testing against real APIs, adding authentication layers, or creating simple proxy servers.

### Key Features

- **Optional OAuth2 Authentication**: Automatically obtains and manages OAuth2 access tokens
- **Token Caching**: Caches tokens to minimize token endpoint calls
- **Custom Headers**: Add custom headers to all relayed requests
- **No Authentication Mode**: Relay requests without any authentication
- **Full Request Forwarding**: Forwards method, path, query params, headers, and body
- **Transparent Response**: Returns exact response from remote server

### Create a Simple Relay Server (Without Authentication)

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "simple-relay",
    "port": 8090,
    "description": "Simple relay to remote API",
    "relayConfig": {
      "remoteUrl": "https://api.example.com"
    }
  }'
```

**Response**: `201 Created`
```json
{
  "serverId": "simple-relay",
  "port": 8090,
  "protocol": "http",
  "baseUrl": "http://localhost:8090",
  "relayEnabled": true,
  "status": "running"
}
```

### Create a Relay Server with OAuth2 Authentication

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "oauth-relay",
    "port": 8090,
    "description": "Relay with OAuth2 authentication",
    "relayConfig": {
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "your-client-id",
      "clientSecret": "your-client-secret",
      "scope": "api:read api:write"
    }
  }'
```

### Create a Relay Server with Custom Headers

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "custom-relay",
    "port": 8090,
    "relayConfig": {
      "remoteUrl": "https://api.example.com",
      "headers": {
        "X-API-Version": "v2",
        "X-Client-App": "test-suite"
      }
    }
  }'
```

### Testing the Relay Server

Once created, any request to the relay server is automatically forwarded to the remote server:

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
- `scope`: OAuth2 scope to request
- `grantType`: OAuth2 grant type (default: "client_credentials")
- `headers`: Custom headers to add to all relayed requests

### Important Notes

- When relay is enabled, **expectations are ignored** - all requests are forwarded
- OAuth2 is completely optional - omit token parameters for no authentication
- If providing OAuth2 config, all three parameters (`tokenUrl`, `clientId`, `clientSecret`) must be provided
- Tokens are automatically cached and refreshed when expired
- Can be combined with TLS/HTTPS and Basic Auth on the mock server side

### Configuration File Examples

**Simple relay without authentication:**
```json
{
  "server": {
    "serverId": "simple-relay",
    "port": 8090,
    "relayConfig": {
      "remoteUrl": "https://api.example.com"
    }
  }
}
```

**Relay with OAuth2:**
```json
{
  "server": {
    "serverId": "oauth-relay",
    "port": 8090,
    "relayConfig": {
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "client-id",
      "clientSecret": "client-secret"
    }
  }
}
```

See `examples/server-config-relay-example.jsonmc` and `examples/server-config-relay-no-auth-example.jsonmc` for complete examples.

For detailed relay configuration documentation, see [docs/RELAY_CONFIGURATION.md](docs/RELAY_CONFIGURATION.md).

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

## Project Structure

```
mock_server/
├── docs/                                    # Documentation
│   ├── FREEMARKER_TEMPLATE_GUIDE.md        # Freemarker templating guide
│   ├── JSONMC_USAGE.md                     # JSON with comments format guide
│   ├── RELAY_CONFIGURATION.md              # Relay/proxy configuration guide
│   └── TEST_SUITE.md                       # Test suite documentation
├── examples/                                # Configuration examples
│   ├── server-config-basicauth-example.jsonmc
│   ├── server-config-example.jsonmc
│   ├── server-config-files-example.jsonmc
│   ├── server-config-mtls-example.jsonmc
│   ├── server-config-relay-example.jsonmc
│   ├── server-config-relay-no-auth-example.jsonmc
│   └── test-config.jsonmc
├── src/
│   ├── main/
│   │   ├── java/io/github/anandb/mockserver/
│   │   │   ├── MockServerApplication.java   # Main application entry point
│   │   │   ├── callback/                    # Response callback implementations
│   │   │   │   ├── FileResponseCallback.java
│   │   │   │   ├── FreemarkerResponseCallback.java
│   │   │   │   └── RelayResponseCallback.java
│   │   │   ├── config/                      # Configuration classes
│   │   │   │   ├── JsonMultilineCommentHttpMessageConverter.java
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/                  # REST API controllers
│   │   │   │   ├── ExpectationController.java
│   │   │   │   └── ServerController.java
│   │   │   ├── exception/                   # Exception handling
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── InvalidCertificateException.java
│   │   │   │   ├── InvalidExpectationException.java
│   │   │   │   ├── ServerAlreadyExistsException.java
│   │   │   │   ├── ServerCreationException.java
│   │   │   │   └── ServerNotFoundException.java
│   │   │   ├── model/                       # Domain models
│   │   │   │   ├── BasicAuthConfig.java
│   │   │   │   ├── CreateServerRequest.java
│   │   │   │   ├── GlobalHeader.java
│   │   │   │   ├── HttpRequestContext.java
│   │   │   │   ├── MtlsConfig.java
│   │   │   │   ├── RelayConfig.java
│   │   │   │   ├── ServerConfiguration.java
│   │   │   │   ├── ServerInfo.java
│   │   │   │   └── TlsConfig.java
│   │   │   ├── service/                     # Business logic services
│   │   │   │   ├── CertificateValidator.java
│   │   │   │   ├── ConfigurationLoaderService.java
│   │   │   │   ├── FreemarkerTemplateService.java
│   │   │   │   ├── MockServerManager.java
│   │   │   │   ├── MockServerOperations.java
│   │   │   │   ├── MockServerOperationsImpl.java
│   │   │   │   ├── OAuth2TokenService.java
│   │   │   │   ├── RelayService.java
│   │   │   │   └── TlsConfigurationService.java
│   │   │   └── util/                        # Utility classes
│   │   │       ├── FreemarkerTemplateDetector.java
│   │   │       ├── JsonCommentParser.java
│   │   │       └── MapperSupplier.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/io/github/anandb/mockserver/
│       │   ├── MockServerApplicationTests.java
│       │   ├── config/
│       │   │   └── JsonMultilineCommentHttpMessageConverterIntegrationTest.java
│       │   ├── controller/
│       │   │   ├── ExpectationControllerTest.java
│       │   │   └── ServerControllerTest.java
│       │   ├── integration/
│       │   │   └── MockServerIntegrationTest.java
│       │   ├── service/
│       │   │   ├── CertificateValidatorTest.java
│       │   │   ├── ConfigurationLoaderServiceJsonmcTest.java
│       │   │   ├── MockServerManagerTest.java
│       │   │   └── TlsConfigurationServiceTest.java
│       │   └── util/
│       │       ├── JsonCommentParserDebugTest.java
│       │       └── JsonCommentParserTest.java
│       └── resources/
│           ├── application-test.properties
│           └── test-server-config.jsonmc
├── .gitignore
├── pom.xml
├── QUICKSTART.md
├── README.md
└── UNLICENSE
```

## Dependencies

- Spring Boot 3.2.0
- MockServer Netty (no-dependencies) 5.15.0
- Lombok (optional)
- Jackson (JSON processing)

## License

This project is provided as-is for demonstration purposes.

## Support

For issues or questions, please refer to:
- [MockServer Documentation](https://www.mock-server.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
