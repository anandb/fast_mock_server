# Freemarker Template Support

## Overview

The mock server now supports Freemarker templates in expectation response bodies. This allows you to create dynamic responses based on incoming HTTP request data.

## Features

When a response body contains Freemarker template syntax, the mock server will automatically:
1. Parse the incoming HTTP request into a context object containing:
   - `headers` - Map of HTTP headers (String → String)
   - `body` - Parsed JSON body (Map of String → Object)
   - `cookies` - Map of cookies (String → String)
2. Evaluate the Freemarker template using this context
3. Return the rendered result as the response body

## Template Detection

A response body is considered a Freemarker template if it contains any of these patterns:
- `${...}` - Variable interpolation
- `<#...>` - Freemarker directives
- `[#...]` - Alternative directive syntax
- `<@...>` - Macro calls
- `[@...]` - Alternative macro syntax

## Usage Examples

### Example 1: Simple Variable Interpolation

**Request:**
```json
POST /api/servers/my-server/expectations
{
  "httpRequest": {
    "method": "POST",
    "path": "/greet"
  },
  "httpResponse": {
    "statusCode": 200,
    "body": "Hello, ${body.name}! You are ${body.age} years old."
  }
}
```

**Test Request:**
```bash
curl -X POST http://localhost:8080/greet \
  -H "Content-Type: application/json" \
  -d '{"name": "John", "age": 30}'
```

**Response:**
```
Hello, John! You are 30 years old.
```

### Example 2: Accessing Headers

**Request:**
```json
POST /api/servers/my-server/expectations
{
  "httpRequest": {
    "method": "GET",
    "path": "/info"
  },
  "httpResponse": {
    "statusCode": 200,
    "body": "User-Agent: ${headers['User-Agent']}\nHost: ${headers['Host']}"
  }
}
```

**Test Request:**
```bash
curl http://localhost:8080/info -H "User-Agent: MyApp/1.0"
```

**Response:**
```
User-Agent: MyApp/1.0
Host: localhost:8080
```

### Example 3: Accessing Cookies

**Request:**
```json
POST /api/servers/my-server/expectations
{
  "httpRequest": {
    "method": "GET",
    "path": "/session"
  },
  "httpResponse": {
    "statusCode": 200,
    "body": "Session ID: ${cookies['JSESSIONID']!('No session')}"
  }
}
```

**Test Request:**
```bash
curl http://localhost:8080/session -H "Cookie: JSESSIONID=abc123"
```

**Response:**
```
Session ID: abc123
```

### Example 4: Conditional Logic

**Request:**
```json
POST /api/servers/my-server/expectations
{
  "httpRequest": {
    "method": "POST",
    "path": "/status"
  },
  "httpResponse": {
    "statusCode": 200,
    "body": "<#if body.status == 'active'>User is active<#else>User is inactive</#if>"
  }
}
```

### Example 5: Complex JSON Response

**Request:**
```json
POST /api/servers/my-server/expectations
{
  "httpRequest": {
    "method": "POST",
    "path": "/user/profile"
  },
  "httpResponse": {
    "statusCode": 200,
    "headers": [
      {"name": "Content-Type", "values": ["application/json"]}
    ],
    "body": "{\"userId\": \"${body.id}\", \"fullName\": \"${body.firstName} ${body.lastName}\", \"email\": \"${body.email}\", \"timestamp\": \"${.now}\"}"
  }
}
```

### Example 6: Iterating Over Lists

**Request:**
```json
POST /api/servers/my-server/expectations
{
  "httpRequest": {
    "method": "POST",
    "path": "/items"
  },
  "httpResponse": {
    "statusCode": 200,
    "body": "<#list body.items as item>${item.name}: $${item.price}<#sep>, </#list>"
  }
}
```

**Test Request:**
```bash
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"name": "Apple", "price": 1.50},
      {"name": "Banana", "price": 0.75},
      {"name": "Orange", "price": 2.00}
    ]
  }'
```

**Response:**
```
Apple: $1.50, Banana: $0.75, Orange: $2.00
```

## Available Context Variables

### headers
Type: `Map<String, String>`

Access HTTP headers from the request. Header names are case-sensitive as received.

Example:
```freemarker
${headers['Content-Type']}
${headers['Authorization']}
```

### body
Type: `Map<String, Object>` (parsed from JSON)

Access the parsed JSON body of the request. Supports nested objects and arrays.

Example:
```freemarker
${body.user.name}
${body.settings.theme}
<#list body.items as item>${item.id}</#list>
```

### cookies
Type: `Map<String, String>`

Access cookies from the request.

Example:
```freemarker
${cookies['sessionId']}
${cookies['theme']!('default')}
```

## Freemarker Tips

1. **Default Values**: Use `!` operator for default values
   ```freemarker
   ${body.name!('Unknown')}
   ```

2. **Null Safety**: Use `??` to check if variable exists
   ```freemarker
   <#if body.name??>Name: ${body.name}</#if>
   ```

3. **Built-in Functions**: Freemarker provides many built-in functions
   ```freemarker
   ${body.name?upper_case}
   ${body.name?length}
   ${.now?string("yyyy-MM-dd HH:mm:ss")}
   ```

4. **Escaping**: Use `?json_string` for JSON escaping
   ```freemarker
   {"message": "${body.text?json_string}"}
   ```

## Error Handling

If template processing fails, the mock server will return:
- Status Code: 500
- Body: "Error processing template: [error message]"

Common errors:
- Invalid Freemarker syntax
- Accessing non-existent variables (use defaults or null checks)
- Invalid JSON in request body (will result in empty body object)

## Notes

- If the request body is not valid JSON, it will be treated as an empty object `{}`
- Only the first value is used if a header has multiple values
- Template processing happens for each incoming request, so keep templates reasonably simple for performance
- Templates are stateless - each request is processed independently
