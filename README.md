# MockServer Manager

A Spring Boot application for managing multiple MockServer instances with support for TLS/mTLS and global response headers.

## Features

- **Multiple MockServer Instances**: Create and manage multiple mock servers in a single process
- **Dynamic Configuration**: Configure expectations via REST API
- **TLS/HTTPS Support**: Enable HTTPS with custom certificates
- **Mutual TLS (mTLS)**: Client certificate validation with CA certificate
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
```

The management API will be available at `http://localhost:8080`

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
