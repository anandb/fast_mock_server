# Agent Development Guide - Mock Server Manager

This document provides essential information for AI agents operating in this repository.

## 🛠 Build and Test Commands

The project uses **Maven** and **Java 25** (see `pom.xml` for current version).

### Build
```bash
mvn clean package          # Build and package JAR
mvn clean install          # Build and install to local repo
mvn compile                # Compile only
```

### Run Application
```bash
mvn spring-boot:run
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmock.server.config.file=./server-config.jsonmc"
```

### Testing
```bash
mvn test                                      # Run all tests
mvn test -Dtest=MockServerManagerTest         # Run single test class
mvn test -Dtest=MockServerManagerTest#testCreateHttpServer  # Run single test method
mvn test -Dtest="*Integration*"               # Run tests matching pattern
```

## Project Structure

```
src/main/java/io/github/anandb/mockserver/
├── callback/       # EnhancedResponseCallback - unified callback handler
├── config/         # Spring configuration classes
├── exception/      # Custom exceptions + GlobalExceptionHandler
├── model/          # DTOs and domain entities
├── service/        # Business logic, MockServer lifecycle
├── strategy/       # Response generation strategies (Strategy Pattern)
└── util/           # Shared utility classes
```

## 📝 Code Style Guidelines

### Formatting
- **Indentation:** 4 spaces (no tabs)
- **Line length:** Max 120 characters
- **Braces:** Always required for conditionals, loops, methods. Optional for one-line lambdas.
- **Blank lines:** One between methods, two between class sections

### Import Order
1. `java.*` and `javax.*` / `jakarta.*`
2. Third-party libraries (org.mockserver, com.fasterxml, etc.)
3. Spring framework imports
4. Project imports (`io.github.anandb.mockserver.*`)
5. Static imports last

Avoid wildcard imports. Use explicit imports.

### Naming Conventions
| Element | Convention | Example |
|---------|------------|----------|
| Classes/Interfaces | PascalCase | `MockServerManager`, `ResponseStrategy` |
| Methods | camelCase | `createServer`, `isTlsEnabled` |
| Variables | camelCase | `serverId`, `tlsConfig` |
| Constants | UPPER_SNAKE_CASE | `DEFAULT_PORT` |
| Packages | lowercase | `io.github.anandb.mockserver.service` |
| DTOs | Suffix with `DTO` | `EnhancedExpectationDTO` |
| Config classes | Suffix with `Config` | `TlsConfig`, `RelayConfig` |
| Strategy interfaces | Suffix with `Strategy` | `ResponseStrategy` |
| Test classes | Suffix with `Test` | `MockServerManagerTest` |

### Lombok Annotations
Use Lombok to reduce boilerplate:
- `@Data` - getters, setters, equals, hashCode, toString
- `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`
- `@Slf4j` - logging (use `log.info()`, `log.warn()`, `log.error()`)
- `@Builder` - builder pattern for complex objects

### Spring Annotations
- `@Service` - business logic services
- `@Component` - general Spring beans (including strategies)
- `@RestController` / `@RestControllerAdvice` - REST endpoints
- `@RequiredArgsConstructor` - constructor injection (preferred over `@Autowired`)

### Documentation
- Use Javadoc for public classes and methods
- Include `@param`, `@return`, `@throws` tags
- Brief description in first sentence

## Error Handling

### Custom Exceptions
Create in `exception/` package, extend `RuntimeException`:
- `ServerNotFoundException` - 404
- `ServerAlreadyExistsException` - 409
- `ServerCreationException` - 500
- `InvalidCertificateException` - 400
- `InvalidExpectationException` - 400

### GlobalExceptionHandler
All exceptions are handled centrally via `@RestControllerAdvice`. Returns structured JSON:
```json
{"errorCode": "ERROR_CODE", "message": "Description", "timestamp": "..."}
```

## Testing Conventions

- **Framework:** JUnit 5 + Mockito
- **Annotations:** `@Test`, `@DisplayName`, `@BeforeEach`, `@AfterEach`
- **Assertions:** Use static imports from `org.junit.jupiter.api.Assertions`
- **Mocking:** Use `@Mock` + `MockitoAnnotations.openMocks(this)`
- **Naming:** Test methods should describe behavior: `testCreateHttpServer`, `shouldRejectDuplicateServerId`

## Strategy Pattern (Response Generation)

Implement `ResponseStrategy` interface:
```java
public interface ResponseStrategy {
    HttpResponse handle(HttpRequest request, EnhancedExpectationDTO config, Map<String, Object> context);
    boolean supports(EnhancedExpectationDTO config);
    default int getPriority() { return 0; }  // Higher = checked first
}
```

Existing strategies:
- `StaticResponseStrategy` - static JSON/text responses
- `DynamicFileStrategy` - file downloads
- `SSEResponseStrategy` - Server-Sent Events
- `RelayResponseStrategy` - proxy to remote servers

To add a new strategy:
1. Create class in `strategy/` implementing `ResponseStrategy`
2. Add `@Component` annotation
3. Implement `supports()` to match your use case
4. Implement `handle()` for response generation

## Configuration

### Supported Formats
- `.json` - standard JSON
- `.jsonmc` - JSON with multiline comments (parsed by `JsonCommentParser`)

### Key Model Classes
- `EnhancedExpectationDTO` - unified expectation configuration
- `ServerCreationRequest` - server creation payload
- `ServerInstance` - runtime server state (record-style accessors)
- `TlsConfig`, `MtlsConfig` - TLS/mTLS configuration
- `RelayConfig` - proxy relay configuration
- `BasicAuthConfig` - basic authentication

## 🤖 AI Agent Notes

- **Context:** Spring Boot wrapper around MockServer Netty. Manages multiple mock server instances on different ports.
- **Key service:** `MockServerManager` - creates, configures, and manages server lifecycle
- **Prefer:** Mapping requests to `EnhancedExpectationDTO` over manual JSON manipulation
- **Features:** TLS/mTLS, Basic Auth, OAuth2 relay, SSE streaming, file downloads, FreeMarker templates
- **Testing:** Always verify changes with `mvn test` before committing

