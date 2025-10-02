# Quick Start Guide

This guide will help you get the MockServer Manager up and running in minutes.

## Prerequisites

Ensure you have installed:
- Java 17 or higher
- Maven 3.8+

## Installation

1. **Clone or navigate to the project directory**
   ```bash
   cd /home/anand/workspace/mock_server
   ```

2. **Build the project**
   ```bash
   mvn clean package
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   The application will start on `http://localhost:8080`

## Quick Test

### 1. Create a Simple HTTP Mock Server

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "test-api",
    "port": 1080,
    "description": "Test API Mock"
  }'
```

### 2. Add an Expectation

```bash
curl -X POST http://localhost:8080/api/servers/test-api/expectations \
  -H "Content-Type: application/json" \
  -d '{
    "httpRequest": {
      "method": "GET",
      "path": "/hello"
    },
    "httpResponse": {
      "statusCode": 200,
      "headers": {
        "Content-Type": ["text/plain"]
      },
      "body": "Hello, World!"
    }
  }'
```

### 3. Test the Mock

```bash
curl http://localhost:1080/hello
```

**Expected Output**: `Hello, World!`

### 4. Verify with Global Headers

Create a server with global headers:

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "api-with-headers",
    "port": 1081,
    "globalHeaders": [
      {"name": "X-API-Version", "value": "1.0"},
      {"name": "X-Mock-Server", "value": "true"}
    ]
  }'
```

Add expectation:

```bash
curl -X POST http://localhost:8080/api/servers/api-with-headers/expectations \
  -H "Content-Type: application/json" \
  -d '{
    "httpRequest": {
      "method": "GET",
      "path": "/user"
    },
    "httpResponse": {
      "statusCode": 200,
      "headers": {
        "Content-Type": ["application/json"]
      },
      "body": {"name": "John Doe"}
    }
  }'
```

Test and check headers:

```bash
curl -v http://localhost:1081/user
```

You should see headers:
- `X-API-Version: 1.0` (from global)
- `X-Mock-Server: true` (from global)
- `Content-Type: application/json` (from expectation)

### 5. List All Servers

```bash
curl http://localhost:8080/api/servers
```

### 6. Clean Up

Delete servers:

```bash
curl -X DELETE http://localhost:8080/api/servers/test-api
curl -X DELETE http://localhost:8080/api/servers/api-with-headers
```

## Next Steps

- See [README.md](README.md) for complete API documentation
- Test TLS/mTLS features using the examples in README
- Explore advanced expectation configurations

## Troubleshooting

**Port already in use?**
```bash
# Check what's using the port
lsof -i :8080

# Change the management port in application.properties
server.port=9090
```

**MockServer not responding?**
```bash
# Check logs
tail -f logs/spring.log

# Verify server is running
curl http://localhost:8080/api/servers
```

## Common Use Cases

### Mock REST API

```json
{
  "httpRequest": {
    "method": "GET",
    "path": "/api/users",
    "queryStringParameters": {
      "page": ["1"]
    }
  },
  "httpResponse": {
    "statusCode": 200,
    "body": {
      "users": [
        {"id": 1, "name": "Alice"},
        {"id": 2, "name": "Bob"}
      ],
      "page": 1,
      "totalPages": 5
    }
  }
}
```

### Mock with Request Matching

```json
{
  "httpRequest": {
    "method": "POST",
    "path": "/api/users",
    "body": {
      "type": "JSON",
      "json": {"name": ".*"}
    }
  },
  "httpResponse": {
    "statusCode": 201,
    "body": {"id": 123, "name": "New User"}
  }
}
```

### Mock Error Response

```json
{
  "httpRequest": {
    "method": "GET",
    "path": "/api/error"
  },
  "httpResponse": {
    "statusCode": 500,
    "body": {"error": "Internal Server Error"}
  }
}
```

Happy Mocking! 🚀
