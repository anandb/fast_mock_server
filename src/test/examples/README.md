# Mock Server Examples

This directory contains example configurations for the mock server, organized into subdirectories. Each example includes:
- A `.jsonmc` configuration file
- `start_server.sh` - Script to start the mock server with the configuration
- `start_client.sh` - Script with curl commands demonstrating the endpoints

## Available Examples

### 1. test-config/
Basic test server configuration demonstrating a simple API endpoint.
- **Port:** 9001
- **Features:** Simple GET endpoint

### 2. server-config-example/
Multi-server configuration with both HTTP and HTTPS servers.
- **Servers:**
  - api-server (HTTP on port 9001)
  - secure-server (HTTPS on port 8443)
- **Features:** Multiple endpoints, TLS/SSL configuration, global headers

### 3. server-config-basicauth-example/
Server with basic authentication.
- **Port:** 9090
- **Features:** Basic authentication (username: admin, password: secret123)
- **Endpoints:** User management API

### 4. server-config-mtls-example/
Server with mutual TLS (mTLS) authentication.
- **Port:** 8443 (HTTPS)
- **Features:** Client certificate verification, embedded certificates
- **Additional Files:** CA certificates, client certificates for testing
- **Endpoints:** Secure banking/transaction API

### 5. server-config-relay-example/
Relay server that forwards requests to a remote API with OAuth2 authentication.
- **Port:** 8090
- **Features:** OAuth2 token acquisition, request forwarding, multiple prefixes support
- **Note:** Update configuration with real OAuth2 credentials and remote URL

### 6. server-config-relay-no-auth-example/
Relay server that forwards requests to a remote API without authentication.
- **Port:** 8090
- **Features:** Simple proxying, custom header injection, multiple prefixes support
- **Note:** Update configuration with real remote URL

### 7. server-config-files-example/
Configuration demonstrating file downloads and multi-part responses.
- **Port:** 8082
- **Features:** Multi-part file downloads
- **Note:** Scripts not yet created for this example

## Usage

Each example can be run independently:

1. **Build the project** (if not already built):
   ```bash
   mvn clean package
   ```

2. **Start a server:**
   ```bash
   cd examples/<example-name>
   ./start_server.sh
   ```

3. **Test the server** (in another terminal):
   ```bash
   cd examples/<example-name>
   ./start_client.sh
   ```

## Directory Structure

```
examples/
├── README.md (this file)
├── test-config/
│   ├── test-config.jsonmc
│   ├── start_server.sh
│   └── start_client.sh
├── server-config-example/
│   ├── server-config-example.jsonmc
│   ├── start_server.sh
│   └── start_client.sh
├── server-config-basicauth-example/
│   ├── server-config-basicauth-example.jsonmc
│   ├── start_server.sh
│   └── start_client.sh
├── server-config-mtls-example/
│   ├── server-config-mtls-example.jsonmc
│   ├── start_server.sh
│   ├── start_client.sh
│   ├── README.md
│   ├── ca-cert.pem
│   ├── ca-key.pem
│   ├── client-cert.pem
│   └── client-key.pem
├── server-config-relay-example/
│   ├── server-config-relay-example.jsonmc
│   ├── start_server.sh
│   └── start_client.sh
├── server-config-relay-no-auth-example/
│   ├── server-config-relay-no-auth-example.jsonmc
│   ├── start_server.sh
│   └── start_client.sh
└── server-config-files-example/
    └── server-config-files-example.jsonmc
```

## Notes

- All start scripts check for the built JAR file in the `target/` directory
- The mTLS example includes sample certificates for testing purposes
- Relay examples require updating the configuration with real remote URLs and credentials
- All bash scripts are executable (`chmod +x` has been applied)
