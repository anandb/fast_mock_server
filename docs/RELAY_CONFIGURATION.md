# Relay Configuration Guide

## Overview

The relay configuration feature allows you to create a mock server that acts as a proxy, forwarding all incoming requests to a remote server with optional OAuth2 authentication. This is useful for:

- Testing against real APIs (with or without OAuth2 authentication)
- Adding authentication layer to existing APIs
- Creating a simple proxy server with custom headers
- Testing OAuth2 token management and caching
- Forwarding requests without authentication

## Features

- **Optional OAuth2 Authentication**: Automatically obtains and manages access tokens using client credentials grant (when configured)
- **Token Caching**: Caches access tokens to minimize token endpoint calls (for OAuth2)
- **Custom Headers**: Add custom headers to all relayed requests
- **Full Request Forwarding**: Forwards all aspects of requests (method, path, query params, headers, body)
- **Transparent Response**: Returns the exact response from the remote server
- **No Authentication Mode**: Relay requests without any authentication when OAuth2 is not configured

## Configuration

### Basic Relay Configuration (Without Authentication)

The simplest relay configuration only requires a remote URL:

```json
{
  "server": {
    "serverId": "simple-relay-server",
    "port": 8080,
    "relayConfig": {
      "remoteUrl": "https://api.example.com"
    }
  }
}
```

### Relay Configuration with OAuth2 Authentication

To add OAuth2 authentication, include the token endpoint and client credentials:

```json
{
  "server": {
    "serverId": "oauth-relay-server",
    "port": 8080,
    "relayConfig": {
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "your-client-id",
      "clientSecret": "your-client-secret"
    }
  }
}
```

### Complete Relay Configuration with All Options

```json
{
  "server": {
    "serverId": "advanced-relay-server",
    "port": 8080,
    "description": "Relay server with full configuration",
    "relayConfig": {
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "your-client-id",
      "clientSecret": "your-client-secret",
      "scope": "api:read api:write",
      "grantType": "client_credentials",
      "headers": {
        "X-Custom-Header": "custom-value",
        "X-API-Version": "v1"
      }
    }
  }
}
```

## Configuration Parameters

### Required Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `remoteUrl` | String | The base URL of the remote server to relay requests to |

### OAuth2 Authentication Parameters (Optional)

To enable OAuth2 authentication, you must provide all three of these parameters. If any are provided, all must be provided:

| Parameter | Type | Description |
|-----------|------|-------------|
| `tokenUrl` | String | The OAuth2 token endpoint URL |
| `clientId` | String | OAuth2 client ID |
| `clientSecret` | String | OAuth2 client secret |

### Optional Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `scope` | String | null | OAuth2 scope to request (space-separated list) |
| `grantType` | String | "client_credentials" | OAuth2 grant type |
| `headers` | Map<String, String> | null | Custom headers to add to all relayed requests |

## How It Works

### Request Flow (Without Authentication)

1. **Incoming Request**: Client sends a request to the mock server
2. **Request Forwarding**:
   - Adds any custom headers from configuration
   - Forwards original request headers (except certain system headers)
   - Forwards request method, path, query parameters, and body
3. **Response Handling**: Returns the exact response from the remote server

### Request Flow (With OAuth2 Authentication)

1. **Incoming Request**: Client sends a request to the mock server
2. **Token Acquisition**: Server checks if a valid access token exists in cache
   - If not cached or expired, fetches a new token from the token endpoint
   - Caches the token for future use (default: 3300 seconds)
3. **Request Forwarding**:
   - Adds `Authorization: Bearer <token>` header
   - Adds any custom headers from configuration
   - Forwards original request headers (except certain system headers)
   - Forwards request method, path, query parameters, and body
4. **Response Handling**: Returns the exact response from the remote server

### Token Caching

- Tokens are cached per server (based on tokenUrl + clientId)
- Default cache duration: 3300 seconds (55 minutes)
- Expired tokens are automatically refreshed on next request
- Cache is cleared when the application shuts down

### Header Handling

#### Headers Added to Relayed Requests

1. `Authorization: Bearer <access_token>` (automatically added when OAuth2 is configured)
2. Custom headers from `relayConfig.headers`
3. Original request headers (with some exclusions)

#### Headers Excluded from Relaying

The following headers are NOT forwarded to prevent conflicts:

- `Host`
- `Connection`
- `Content-Length`
- `Transfer-Encoding`
- `Authorization` (original, since we add our own)

## Usage Examples

### Example 1: Simple Relay Without Authentication

Create a server configuration file `relay-no-auth-config.jsonmc`:

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

Start the server:
```bash
curl -X POST http://localhost:8080/api/servers/load \
  -H "Content-Type: application/json" \
  -d @relay-no-auth-config.jsonmc
```

Test the relay:
```bash
curl http://localhost:8090/users/123
# Request is forwarded to https://api.example.com/users/123 without authentication
```

### Example 2: API Relay with OAuth2 Authentication

Create a server configuration file `relay-oauth-config.jsonmc`:

```json
{
  "server": {
    "serverId": "api-relay",
    "port": 8090,
    "relayConfig": {
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "my-client-id",
      "clientSecret": "my-client-secret"
    }
  }
}
```

Start the server:
```bash
curl -X POST http://localhost:8080/api/servers/load \
  -H "Content-Type: application/json" \
  -d @relay-config.jsonmc
```

Test the relay:
```bash
curl http://localhost:8090/users/123
# Request is forwarded to https://api.example.com/users/123 with OAuth2 token
```

### Example 3: Relay with Custom Headers (No Authentication)

```json
{
  "server": {
    "serverId": "relay-custom-headers",
    "port": 8090,
    "relayConfig": {
      "remoteUrl": "https://api.example.com",
      "headers": {
        "X-API-Version": "v2",
        "X-Client-App": "test-suite"
      }
    }
  }
}
```

### Example 4: Relay with OAuth2 and Custom Headers

```json
{
  "server": {
    "serverId": "api-relay-custom",
    "port": 8090,
    "relayConfig": {
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "my-client-id",
      "clientSecret": "my-client-secret",
      "headers": {
        "X-API-Version": "v2",
        "X-Client-App": "test-suite"
      }
    }
  }
}
```

### Example 5: Relay with OAuth2 Scope

```json
{
  "server": {
    "serverId": "api-relay-scoped",
    "port": 8090,
    "relayConfig": {
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "my-client-id",
      "clientSecret": "my-client-secret",
      "scope": "read:users write:users admin:all"
    }
  }
}
```

## REST API Usage

### Create a Relay Server via API (Without Authentication)

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "simple-relay",
    "port": 8090,
    "relayConfig": {
      "remoteUrl": "https://api.example.com"
    }
  }'
```

### Create a Relay Server via API (With OAuth2)

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "oauth-relay",
    "port": 8090,
    "relayConfig": {
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "client-id",
      "clientSecret": "client-secret"
    }
  }'
```

### Check Server Status

```bash
curl http://localhost:8080/api/servers/relay-server
```

Response will include `"relayEnabled": true`:
```json
{
  "serverId": "relay-server",
  "port": 8090,
  "protocol": "http",
  "baseUrl": "http://localhost:8090",
  "relayEnabled": true,
  "status": "running"
}
```

## Important Notes

1. **Expectations are Ignored**: When relay is enabled, any expectations configured for the server are ignored. ALL requests are forwarded to the remote server.

2. **OAuth2 is Optional**: OAuth2 authentication is completely optional. You can create a relay without any authentication by only providing the `remoteUrl`.

3. **Partial OAuth2 Config Not Allowed**: If you provide any OAuth2 parameter (`tokenUrl`, `clientId`, or `clientSecret`), you must provide all three. Partial configuration will result in a validation error.

4. **OAuth2 Support**: When OAuth2 is enabled, currently only supports `client_credentials` grant type. Other grant types can be specified but the implementation handles client credentials flow.

5. **Error Handling**: If relay fails (e.g., token acquisition fails, remote server is unreachable), the mock server returns a 502 Bad Gateway error with details.

6. **Security**: Client secrets are stored in memory. Use environment variables or secure configuration management in production.

7. **Performance**: When OAuth2 is enabled, token caching significantly reduces overhead. First request may be slower due to token acquisition.

## Troubleshooting

### Issue: 502 Bad Gateway Error

**Possible Causes:**
- Token URL is incorrect or unreachable
- Invalid client credentials
- Remote URL is incorrect or unreachable
- Network connectivity issues

**Solution:** Check server logs for detailed error messages.

### Issue: Token Expires Too Quickly

**Solution:** The token cache duration is set to 3300 seconds (55 minutes) by default. If your token expires sooner, the system will automatically fetch a new one. Check with your OAuth2 provider about token lifetime.

### Issue: Custom Headers Not Being Forwarded

**Solution:** Verify that the headers are correctly specified in the `relayConfig.headers` object. Note that certain system headers (Host, Connection, etc.) are intentionally excluded.

## Advanced Topics

### Combining Relay with Other Features

Relay configuration can be combined with:
- **TLS/HTTPS**: The mock server can accept HTTPS connections while relaying to HTTP or HTTPS
- **Global Headers**: Global headers configured on the server are NOT added to relayed requests (only custom headers from relayConfig are added)
- **Basic Auth**: The mock server can require basic auth for incoming requests, separate from the OAuth2 used for the remote server

Example with TLS:
```json
{
  "server": {
    "serverId": "secure-relay",
    "port": 8443,
    "tlsConfig": {
      "keystore": "path/to/keystore.jks",
      "keystorePassword": "password"
    },
    "relayConfig": {
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "client-id",
      "clientSecret": "client-secret"
    }
  }
}
```

## See Also

- [Server Configuration Guide](../README.md)
- [API Documentation](../QUICKSTART.md)
- [Relay with OAuth2 Example](../examples/server-config-relay-example.jsonmc)
- [Relay without Authentication Example](../examples/server-config-relay-no-auth-example.jsonmc)
