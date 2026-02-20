# Relay Configuration Guide

## Overview

The relay configuration feature allows you to create a mock server that acts as a proxy, forwarding all incoming requests to a remote server with optional OAuth2 authentication. This is useful for:

- Testing against real APIs (with or without OAuth2 authentication)
- Adding authentication layer to existing APIs
- Creating a simple proxy server with custom headers
- Testing OAuth2 token management and caching
- Forwarding requests without authentication
- Accessing services inside Kubernetes clusters via kubectl tunnel

## Features

- **Optional OAuth2 Authentication**: Automatically obtains and manages access tokens using client credentials grant (when configured)
- **Token Caching**: Caches access tokens to minimize token endpoint calls (for OAuth2)
- **Custom Headers**: Add custom headers to all relayed requests
- **Full Request Forwarding**: Forwards all aspects of requests (method, path, query params, headers, body)
- **Transparent Response**: Returns the exact response from the remote server
- **No Authentication Mode**: Relay requests without any authentication when OAuth2 is not configured
- **Kubernetes Tunnel Support**: Establish kubectl port-forward tunnels to Kubernetes pods

## Configuration

### Basic Relay Configuration (Without Authentication)

The simplest relay configuration only requires a remote URL:

```json
{
  "server": {
    "serverId": "simple-relay-server",
    "port": 8080,
    "relays": [{
      "remoteUrl": "https://api.example.com"
    }]
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
    "relays": [{
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "@{OAUTH_CLIENT_ID}",
      "clientSecret": "@{OAUTH_CLIENT_SECRET}"
    }]
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
    "relays": [{
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
    }]
  }
}
```

## Configuration Parameters

### Required Parameters (one of remoteUrl or tunnelConfig)

| Parameter | Type | Description |
|-----------|------|-------------|
| `remoteUrl` | String | The base URL of the remote server to relay requests to |
| **OR** |||
| `tunnelConfig` | Object | Kubernetes tunnel configuration (namespace, podPrefix, podPort) |

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
| `prefixes` | List<String> | `["/**"]` | List of path prefixes to match (ant patterns). When multiple relays match, the longest matching prefix is selected |
| `scope` | String | null | OAuth2 scope to request (space-separated list) |
| `grantType` | String | "client_credentials" | OAuth2 grant type |
| `headers` | Map<String, String> | null | Custom headers to add to all relayed requests |

### Multiple Prefixes and Longest Match

Each relay configuration can have multiple prefixes. When a request comes in, the server checks all configured prefixes across all relays and selects the **longest matching prefix**.

Example:
```json
{
  "relays": [
    {
      "prefixes": ["/api/v1", "/api"],
      "remoteUrl": "https://api-v1.example.com"
    },
    {
      "prefixes": ["/images"],
      "remoteUrl": "https://images.example.com"
    }
  ]
}
```

- Request to `/api/users` → matches `/api` → forwards to `https://api-v1.example.com`
- Request to `/api/v1/users` → matches `/api/v1` (longer) → forwards to `https://api-v1.example.com`
- Request to `/images/logo.png` → matches `/images` → forwards to `https://images.example.com`

If no prefixes are specified, defaults to `["/**"]` (matches all paths).

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
2. Custom headers from `relays[].headers`
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
    "relays": [{
      "remoteUrl": "https://api.example.com"
    }]
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
    "relays": [{
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "my-client-id",
      "clientSecret": "my-client-secret"
    }]
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

### Example 4: Relay with OAuth2 and Custom Headers

```json
{
  "server": {
    "serverId": "api-relay-custom",
    "port": 8090,
    "relays": [{
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "my-client-id",
      "clientSecret": "my-client-secret",
      "headers": {
        "X-API-Version": "v2",
        "X-Client-App": "test-suite"
      }
    }]
  }
}
```

### Example 5: Relay with OAuth2 Scope

```json
{
  "server": {
    "serverId": "api-relay-scoped",
    "port": 8090,
    "relays": [{
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "my-client-id",
      "clientSecret": "my-client-secret",
      "scope": "read:users write:users admin:all"
    }]
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
    "relays": [{
      "remoteUrl": "https://api.example.com"
    }]
  }'
```

### Create a Relay Server via API (With OAuth2)

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{
    "serverId": "oauth-relay",
    "port": 8090,
    "relays": [{
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "client-id",
      "clientSecret": "client-secret"
    }]
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

2. **Multiple Prefixes**: Each relay can have multiple prefixes. The **longest matching prefix** is selected when multiple relays match.

3. **Default Prefix**: If no prefixes are specified, defaults to `["/**"]` which matches all paths.

4. **OAuth2 is Optional**: OAuth2 authentication is completely optional. You can create a relay without any authentication by only providing the `remoteUrl`.

3. **Partial OAuth2 Config Not Allowed**: If you provide any OAuth2 parameter (`tokenUrl`, `clientId`, or `clientSecret`), you must provide all three. Partial configuration will result in a validation error.

4. **OAuth2 Support**: When OAuth2 is enabled, currently only supports `client_credentials` grant type. Other grant types can be specified but the implementation handles client credentials flow.

5. **Error Handling**: If relay fails (e.g., token acquisition fails, remote server is unreachable), the mock server returns a 502 Bad Gateway error with details.

6. **Security**: Client secrets are stored in memory. Use environment variables (`@{VARIABLE}` syntax) or secure configuration management in production.

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

**Solution:** Verify that the headers are correctly specified in the `relays[].headers` object. Note that certain system headers (Host, Connection, etc.) are intentionally excluded.

## Advanced Topics

### Combining Relay with Other Features

Relay configuration can be combined with:
- **TLS/HTTPS**: The mock server can accept HTTPS connections while relaying to HTTP or HTTPS
- **Global Headers**: Global headers configured on the server are NOT added to relayed requests (only custom headers from relays are added)
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
    "relays": [{
      "remoteUrl": "https://api.example.com",
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "client-id",
      "clientSecret": "client-secret"
    }]
  }
}
```

## See Also

- [Server Configuration Guide](../README.md)
- [Relay with OAuth2 Example](../examples/server-config-relay-example.jsonmc)
- [Relay without Authentication Example](../examples/server-config-relay-no-auth-example.jsonmc)

---

# Kubernetes Tunnel Configuration

## Overview

The Kubernetes tunnel feature allows you to create a relay server that automatically establishes a `kubectl port-forward` tunnel to a Kubernetes pod. This enables access to services running inside a Kubernetes cluster without exposing them publicly.

## Features

- **Automatic Pod Discovery**: Programmatically discovers pods using a prefix filter
- **Single Pod Selection**: Automatically selects the first matching pod
- **Auto-assigned Ports**: Host port is automatically assigned from range 9000-11000
- **Sequential Tunnel Creation**: Multiple tunnels are created one at a time to avoid port conflicts
- **Graceful Shutdown**: Tunnels are automatically killed when the server stops
- **Tunnel over Remote URL**: When tunnel is configured, it takes precedence over remoteUrl

## Configuration

### Basic Tunnel Configuration

```json
{
  "server": {
    "serverId": "k8s-tunnel-server",
    "port": 9001,
    "relays": [{
      "tunnelConfig": {
        "namespace": "default",
        "podPrefix": "my-app-",
        "podPort": 8080
      },
      "prefixes": ["/api/**"]
    }]
  }
}
```

### Tunnel Configuration with OAuth2

```json
{
  "server": {
    "serverId": "k8s-tunnel-oauth",
    "port": 9002,
    "relays": [{
      "tunnelConfig": {
        "namespace": "production",
        "podPrefix": "api-service-",
        "podPort": 8080
      },
      "tokenUrl": "https://auth.example.com/oauth/token",
      "clientId": "your-client-id",
      "clientSecret": "your-client-secret"
    }]
  }
}
```

## Configuration Parameters

### Required Parameters (one of remoteUrl or tunnelConfig)

| Parameter | Type | Description |
|-----------|------|-------------|
| `remoteUrl` | String | The base URL of the remote server to relay requests to |
| **OR** |||
| `tunnelConfig` | Object | Kubernetes tunnel configuration (see below) |

### Tunnel Configuration Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `namespace` | String | Kubernetes namespace where pods are located |
| `podPrefix` | String | Prefix to filter pods (first matching pod is selected) |
| `podPort` | Integer | Target port within the Kubernetes pod |

### Optional Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `prefixes` | List<String> | `["/**"]` | List of path prefixes to match (ant patterns) |
| `tokenUrl` | String | null | OAuth2 token endpoint URL |
| `clientId` | String | null | OAuth2 client ID |
| `clientSecret` | String | null | OAuth2 client secret |
| `scope` | String | null | OAuth2 scope to request |
| `grantType` | String | "client_credentials" | OAuth2 grant type |
| `headers` | Map<String, String> | null | Custom headers to add to all relayed requests |

## How It Works

### Startup Flow

1. **kubectl Validation**: On first tunnel startup, validates that kubectl is installed and accessible
2. **Pod Discovery**: Executes `kubectl get pods -n <namespace>` and filters by prefix
3. **Port Assignment**: Automatically finds an available port in range 9000-11000
4. **Tunnel Creation**: Starts `kubectl port-forward pod/<podName> <hostPort>:<podPort> -n <namespace>`
5. **Relay Setup**: Configures relay to forward to `http://localhost:<hostPort>`

### Request Flow

1. **Incoming Request**: Client sends a request to the mock server
2. **Tunnel Check**: If tunnel is enabled, uses `localhost:<assignedHostPort>`
3. **Request Forwarding**: Forwards to the Kubernetes pod via the tunnel
4. **Response Handling**: Returns the exact response from the pod

### Shutdown Flow

1. **Server Stop**: When the mock server is deleted or application shuts down
2. **Tunnel Cleanup**: All kubectl processes are forcibly terminated
3. **Port Release**: Ports are released back to the operating system

## Important Notes

1. **kubectl Requirement**: kubectl must be installed and configured with access to the target Kubernetes cluster
2. **Port Range**: Host ports are automatically assigned from 9000-11000
3. **Sequential Creation**: Multiple tunnels are created one at a time to avoid port conflicts
4. **First Pod Match**: If multiple pods match the prefix, the first one is selected
5. **Tunnel Precedence**: When tunnelConfig is present, it takes precedence over remoteUrl
6. **OAuth2 Compatible**: Tunnels can be combined with OAuth2 authentication

## Usage Example

### Example: Accessing a Kubernetes Service

Given a Kubernetes deployment:
- Namespace: `production`
- Pod prefix: `my-api-`
- Container port: `8080`

Configuration:
```json
{
  "server": {
    "serverId": "k8s-relay",
    "port": 9001,
    "relays": [{
      "tunnelConfig": {
        "namespace": "production",
        "podPrefix": "my-api-",
        "podPort": 8080
      },
      "prefixes": ["/api/**"]
    }]
  }
}
```

Start the server:
```bash
java -Dmock.server.config.file=./k8s-tunnel-config.jsonmc -jar target/mock-server-1.0.0.jar
```

Test the relay:
```bash
curl http://localhost:9001/api/users
# Request is forwarded via kubectl tunnel to the pod at my-api-xxx in namespace production
```

## Troubleshooting

### Issue: "No pod found matching prefix"

**Possible Causes:**
- No pods running in the specified namespace
- Pod prefix doesn't match any pod names
- kubectl not configured with correct context

**Solution:**
- Verify pods exist: `kubectl get pods -n <namespace>`
- Check kubectl context: `kubectl config current-context`

### Issue: "kubectl is not installed or not accessible"

**Solution:**
- Install kubectl: `curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"`
- Configure kubectl with valid kubeconfig

### Issue: "Failed to find available port"

**Possible Causes:**
- All ports in range 9000-11000 are in use

**Solution:**
- Stop other services using ports in that range
- Modify the port range in KubernetesTunnelService if needed

## Security Considerations

1. **kubectl Access**: The server process needs access to kubectl and appropriate Kubernetes credentials
2. **Network Access**: Must have network access to the Kubernetes API server
3. **Pod Access**: Must have permission to port-forward to the target pods
