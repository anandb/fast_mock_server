# Path Variables Example

This example demonstrates how to use path variables in Freemarker templates within the Mock Server.

## Overview

Path variables allow you to extract dynamic segments from URL paths (e.g., `/users/{id}`) and use them in Freemarker templates along with headers, body, and cookies.

## Features Demonstrated

- **Simple path variables**: Single variable extraction (e.g., `/users/{id}`)
- **Multiple path variables**: Multiple variables in a single path (e.g., `/users/{userId}/posts/{postId}`)
- **Complex nested paths**: Variables in deeply nested paths (e.g., `/api/v1/organizations/{orgId}/projects/{projectId}/tasks`)
- **Integration with other context**: Combining path variables with headers, request body, and cookies in templates

## Running the Example

### 1. Start the Mock Server

```bash
./start_server.sh
```

This will start the mock server on port 9090 with the example configuration loaded.

### 2. Test the Endpoints

In a separate terminal, run:

```bash
./start_client.sh
```

This will execute a series of test requests demonstrating various path variable scenarios.

## Example Endpoints

### Simple Path Variable

**Request:**
```bash
GET http://localhost:9090/users/123
```

**Template:**
```json
{
  "userId": "${pathVariables.id}",
  "message": "User details retrieved"
}
```

**Response:**
```json
{
  "userId": "123",
  "message": "User details retrieved"
}
```

### Multiple Path Variables

**Request:**
```bash
GET http://localhost:9090/users/456/posts/789
```

**Template:**
```json
{
  "userId": "${pathVariables.userId}",
  "postId": "${pathVariables.postId}",
  "title": "Post ${pathVariables.postId} by user ${pathVariables.userId}"
}
```

**Response:**
```json
{
  "userId": "456",
  "postId": "789",
  "title": "Post 789 by user 456"
}
```

### Combining with Request Body

**Request:**
```bash
POST http://localhost:9090/api/v1/organizations/org123/projects/proj456/tasks
Content-Type: application/json

{
  "title": "Implement new feature",
  "description": "Add path variable support",
  "priority": "high"
}
```

**Template:**
```json
{
  "organizationId": "${pathVariables.orgId}",
  "projectId": "${pathVariables.projectId}",
  "title": "${body.title}",
  "description": "${body.description}",
  "priority": "${body.priority}"
}
```

**Response:**
```json
{
  "organizationId": "org123",
  "projectId": "proj456",
  "title": "Implement new feature",
  "description": "Add path variable support",
  "priority": "high"
}
```

## Freemarker Context

Path variables are available in the Freemarker template context under the `pathVariables` object:

```freemarker
${pathVariables.variableName}
```

### Complete Context Available in Templates

- `pathVariables` - Map of path variable names to values
- `headers` - Map of HTTP headers
- `body` - Parsed JSON request body (as a Map)
- `cookies` - Map of cookies

### Example Using All Context Types

```json
{
  "userId": "${pathVariables.id}",
  "userName": "${headers['X-User-Name']!'anonymous'}",
  "requestData": {
    "name": "${body.name}",
    "age": ${body.age}
  },
  "sessionId": "${cookies['sessionId']!'no-session'}"
}
```

## Tips

1. **Default Values**: Use Freemarker's default value operator `!` to provide fallback values:
   ```freemarker
   ${pathVariables.id!'unknown'}
   ```

2. **Path Pattern Matching**: The path pattern in your expectation must exactly match the request path structure:
   - Pattern: `/users/{id}` ✓ Matches: `/users/123`
   - Pattern: `/users/{id}` ✗ Does NOT match: `/users/123/profile`

3. **Variable Naming**: Use descriptive variable names in your path patterns:
   - Good: `/users/{userId}/posts/{postId}`
   - Avoid: `/users/{id}/posts/{id2}`

4. **Testing**: Always test your path variable patterns with the actual requests you expect to receive.

## Configuration File

See [server-config-pathvars-example.jsonmc](./server-config-pathvars-example.jsonmc) for the complete configuration with detailed examples.
