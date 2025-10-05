# application/jsonmc MIME Type Usage Guide

## Overview

The Mock Server now supports a custom MIME type `application/jsonmc` (JSON with Multi-line strings and Comments) that allows you to send HTTP requests with JSON containing:

- **C++ style comments**: Both single-line (`//`) and multi-line (`/* */`) comments
- **Multiline strings**: Using triple quotes (`"""`) for strings that span multiple lines

## How It Works

When a client sends an HTTP request with `Content-Type: application/jsonmc`, the server:

1. Parses the JSON with comments and multiline strings using `JsonCommentParser`
2. Converts it to standard JSON
3. Processes it through the normal Spring MVC pipeline (validation, deserialization, etc.)

## Usage Example

### Sending a Request

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/jsonmc" \
  -d '{
    // Server configuration with comments
    "serverId": "my-test-server",

    /* Port configuration
       Using port 9090 for this server */
    "port": 9090,

    // Description using multiline string
    "description": """This is a test server
    that demonstrates the use of
    multiline strings and comments
    in JSON configuration"""
  }'
```

### More Complex Example

```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/jsonmc" \
  -d '{
    // Basic server settings
    "serverId": "secure-server",
    "port": 8443,

    /* TLS Configuration
     * Enable TLS for secure communication
     * This requires valid certificates */
    "tlsConfig": {
      "enabled": true,
      "keyStorePath": "/path/to/keystore.p12",
      "keyStorePassword": "secret"
    },

    // Global headers for all responses
    "globalHeaders": [
      {
        "name": "X-Server-ID",
        "value": "secure-server"
      }
    ],

    // Multiline description
    "description": """Production server with:
    - TLS enabled
    - Custom headers
    - High security settings"""
  }'
```

## Syntax Reference

### Single-Line Comments

```json
{
  // This is a single-line comment
  "key": "value"
}
```

### Multi-Line Comments

```json
{
  /* This is a
     multi-line comment
     spanning several lines */
  "key": "value"
}
```

### Multiline Strings

```json
{
  "description": """This is a multiline string.
  It can span multiple lines.
  Line breaks are preserved as \n in the final JSON."""
}
```

## Important Notes

1. **Content-Type Header**: You must set `Content-Type: application/jsonmc` for the server to use this parser
2. **Response Format**: Responses are still sent as regular `application/json`
3. **Validation**: All Spring validation annotations still apply after parsing
4. **Error Handling**: Invalid JSONMC syntax will return a 400 Bad Request error
5. **Escape Sequences**: Multiline strings automatically escape:
   - Newlines (`\n`)
   - Double quotes (`\"`)
   - Backslashes (`\\`)
   - Other standard JSON escape sequences

## Implementation Details

The implementation consists of three main components:

1. **JsonCommentParser** (`src/main/java/io/github/anandb/mockserver/util/JsonCommentParser.java`)
   - Core parser that removes comments and converts multiline strings

2. **JsonMultilineCommentHttpMessageConverter** (`src/main/java/io/github/anandb/mockserver/config/JsonMultilineCommentHttpMessageConverter.java`)
   - Spring HTTP message converter that handles `application/jsonmc` content type

3. **WebConfig** (`src/main/java/io/github/anandb/mockserver/config/WebConfig.java`)
   - Registers the custom converter with Spring MVC

## Testing

Integration tests are available in:
`src/test/java/io/github/anandb/mockserver/config/JsonMultilineCommentHttpMessageConverterIntegrationTest.java`

Run tests with:
```bash
mvn test -Dtest=JsonMultilineCommentHttpMessageConverterIntegrationTest
```

## Benefits

- **Better Readability**: Add comments to document your JSON configurations
- **Easier Maintenance**: Explain complex configurations inline
- **Multiline Strings**: No need to escape newlines in long text blocks
- **Backward Compatible**: Regular JSON still works with standard `application/json` content type
