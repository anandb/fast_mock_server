# MockServer Manager

A Spring Boot application for managing multiple MockServer instances with support for TLS/mTLS and global response headers.

## Features

- **Multiple MockServer Instances**: Create and manage multiple mock servers in a single process
- **Dynamic Configuration**: Configure expectations via REST API
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

## Loading Configuration from File

You can automatically create servers and configure expectations at startup by providing a JSON configuration file. This is useful for:
- Consistent test environments
- CI/CD pipelines
- Automated testing setups
- Quick server setup without API calls

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
    "files": [
      "/absolute/path/to/document1.pdf",
      "/absolute/path/to/document2.pdf"
    ],
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
    "files": ["/path/to/report.pdf"]
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
    "files": [
      "/path/to/data.csv",
      "/path/to/summary.json",
      "/path/to/archive.zip"
    ]
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
logging.level.com.example.mockserver=DEBUG
```

## Project Structure

```
mock_server/
├── src/
│   ├── main/
│   │   ├── java/com/example/mockserver/
│   │   │   ├── MockServerApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── ServerController.java
│   │   │   │   └── ExpectationController.java
│   │   │   ├── service/
│   │   │   │   ├── MockServerManager.java
│   │   │   │   ├── TlsConfigurationService.java
│   │   │   │   └── CertificateValidator.java
│   │   │   ├── model/
│   │   │   │   ├── CreateServerRequest.java
│   │   │   │   ├── TlsConfig.java
│   │   │   │   ├── MtlsConfig.java
│   │   │   │   ├── GlobalHeader.java
│   │   │   │   └── ServerInfo.java
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       └── [Custom exceptions]
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── pom.xml
└── README.md
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
