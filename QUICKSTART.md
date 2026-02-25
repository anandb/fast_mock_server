# Quick Start Guide

This guide will help you get the MockServer Manager up and running in minutes.

## Prerequisites

Ensure you have installed:
- Java 17 or higher
- Maven 3.8+

## Installation

1. **Clone or navigate to the project directory**
   ```bash
   cd fast_mock_server
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

### 1. Create a Configuration File

Create a file named `server-config.jsonmc`:

```json
[
  {
    "server": {
      "serverId": "test-api",
      "port": 1080,
      "description": "Test API Mock"
    },
    "expectations": [
      {
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
      }
    ]
  }
]
```

### 2. Run with Configuration

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmock.server.config.file=./server-config.jsonmc"
```

### 3. Test the Mock

```bash
curl http://localhost:1080/hello
```

**Expected Output**: `Hello, World!`

### 4. Verify with Global Headers

Update the configuration file:

```json
[
  {
    "server": {
      "serverId": "api-with-headers",
      "port": 1081,
      "globalHeaders": [
        {"name": "X-API-Version", "value": "1.0"},
        {"name": "X-Mock-Server", "value": "true"}
      ]
    },
    "expectations": [
      {
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
      }
    ]
  }
]
```

Restart the server and test:

```bash
curl -v http://localhost:1081/user
```

You should see headers:
- `X-API-Version: 1.0` (from global)
- `X-Mock-Server: true` (from global)
- `Content-Type: application/json` (from expectation)

## Next Steps

- See [README.md](README.md) for complete configuration documentation
- Explore example configurations in `examples/` directory
- Test TLS/mTLS features using the examples in README

## Troubleshooting

**Port already in use?**
```bash
# Check what's using the port
lsof -i :8080

# Change the mock server port in your config file
```

**Server not responding?**
```bash
# Check logs
tail -f logs/spring.log

# Verify configuration file syntax
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

Happy Mocking!
