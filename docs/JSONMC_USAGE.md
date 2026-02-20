# JSONMC (JSON with Multiline strings and Comments) Usage Guide

## Overview

The Mock Server supports a custom format called **JSONMC** (JSON with Multiline strings and Comments). This is a custom extension of JSON designed to make configuration files more human-readable and maintainable.

It solves two major pain points of standard JSON: **lack of comments** and **difficult-to-read multiline strings**.

### Key Features

- **Standard Comments**: Supports both single-line (`//`) and multi-line (`/* ... */`) comments, identical to Java or JavaScript.
- **Backtick Multiline Strings**: Uses backticks (`` ` ``) for strings that span multiple lines (similar to JavaScript template literals). This eliminates the need for escaping newlines (`\n`) or manually escaping double quotes within large blocks of text like JSON bodies or certificates.
- **Strict JSON Compatibility**: Before being parsed by the application, the system pre-processes the content to strip comments and convert backtick strings into standard escaped JSON strings.

## How It Works

The system handles JSONMC in two primary ways:

1.  **File Loading**: The `ConfigurationLoaderService` automatically detects the format. If a filename ends in `.jsonmc` or if the content contains comment markers (`//` or `/*`), it triggers the enhanced parser.
2.  **API Requests**: When a client sends an HTTP request with `Content-Type: application/jsonmc`, the server:
    - Parses the content using `JsonCommentParser`
    - Converts it to standard JSON
    - Processes it through the normal Spring MVC pipeline (validation, deserialization, etc.)

## Usage Example

### Comparison

#### Standard JSON (Difficult to maintain)
```json
{
  "description": "Line 1\nLine 2\nLine 3 with \"quotes\"",
  "certificate": "-----BEGIN CERTIFICATE-----\nMIID...\n-----END CERTIFICATE-----"
}
```

#### JSONMC (Easy to read)
```javascript
{
  // This is a single-line comment
  "description": `
    Line 1
    Line 2
    Line 3 with "quotes"
  `,

  /*
     Multi-line comments are great
     for certificates
  */
  "certificate": `-----BEGIN CERTIFICATE-----
MIID...
-----END CERTIFICATE-----`
}
```

### Sending an API Request

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
    "description": `This is a test server
    that demonstrates the use of
    multiline strings and comments
    in JSON configuration`
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
  "description": `This is a multiline string.
  It can span multiple lines.
  Line breaks are preserved as \n in the final JSON.`
}
```

### Environment Variable Expansion
Environment variables can be referenced using the `@{VARIABLE}` or `@{VARIABLE:-DEFAULT}` syntax anywhere in the configuration. Expansion happens *before* JSON parsing, allowing it to be used for any value.

```json
{
  "server": {
    "serverId": "api-server",
    "port": @{SERVER_PORT:-8080}
  },
  "relays": [
    {
      "remoteUrl": "@{REMOTE_API_URL}",
      "clientSecret": "@{API_CLIENT_SECRET}"
    }
  ]
}
```

> [!IMPORTANT]
> This syntax (`@{}`) is specifically chosen to avoid conflicts with FreeMarker template syntax (`${}`), which is used within response bodies and file paths.

## Important Notes

1.  **Content-Type Header**: You must set `Content-Type: application/jsonmc` for the API to use this parser.
2.  **Response Format**: Responses are still sent as regular `application/json`.
3.  **Validation**: All Spring validation annotations still apply after parsing.
4.  **Error Handling**: Invalid JSONMC syntax will return a 400 Bad Request error.
5.  **Escape Sequences**: Multiline strings automatically escape newlines (`\n`), double quotes (`\"`), and backslashes (`\\`).

## Implementation Details

The implementation consists of three main components:

1.  **JsonCommentParser** (`src/main/java/io/github/anandb/mockserver/util/JsonCommentParser.java`)
    - Core utility that uses regex to strip comments and escape content between backticks.
2.  **JsonMultilineCommentHttpMessageConverter** (`src/main/java/io/github/anandb/mockserver/config/JsonMultilineCommentHttpMessageConverter.java`)
    - Spring HTTP message converter that handles the `application/jsonmc` content type.
3.  **WebConfig** (`src/main/java/io/github/anandb/mockserver/config/WebConfig.java`)
    - Registers the custom converter with Spring MVC.

## Testing

Integration tests are available in:
`src/test/java/io/github/anandb/mockserver/config/JsonMultilineCommentHttpMessageConverterIntegrationTest.java`

Run tests with:
```bash
mvn test -Dtest=JsonMultilineCommentHttpMessageConverterIntegrationTest
```

## Benefits

-   **Better Readability**: Add comments to document your JSON configurations.
-   **Easier Maintenance**: Explain complex configurations inline.
-   **Clean Certificates**: Copy-paste PEM certificates directly without manual formatting or escaping.
-   **Backward Compatible**: Regular JSON still works with the standard `application/json` content type.
