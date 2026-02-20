# Agent Development Guide - Mock Server Manager

This document provides essential information for AI agents operating in this repository.

## 🛠 Build and Test Commands

The project uses Maven and Java 17.

- **Build and package:** `mvn clean package`
- **Run application:** `mvn spring-boot:run`
- **Run all tests:** `mvn test`
- **Run a single test class:** `mvn test -Dtest=ClassName`
- **Run a single test method:** `mvn test -Dtest=ClassName#methodName`
- **Run with specific config file:** `mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmock.server.config.file=./server-config.jsonmc"`

## 📝 Code Style & Conventions

### 1. Project Structure
- **Controller:** REST API endpoints in `src/main/java/.../controller/`
- **Service:** Business logic and MockServer lifecycle in `src/main/java/.../service/`
- **Model:** Request/Response DTOs and domain entities in `src/main/java/.../model/`
- **Strategy:** Response generation strategies in `src/main/java/.../strategy/`
- **Exception:** Custom exceptions and global handler in `src/main/java/.../exception/`
- **Callback:** Unified `EnhancedResponseCallback` in `src/main/java/.../callback/`
- **Util:** Shared utility classes in `src/main/java/.../util/`

### 2. Code Style
- **Indentation:** 4 spaces (no tabs)
- **Line Length:** Max 120 characters
- **Lombok:** Use `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, `@Slf4j` to reduce boilerplate
- **Spring:** Use `@Service`, `@Component`, `@Controller` annotations for dependency injection
- **Logging:** Use Lombok's `@Slf4j` and `log.info/warn/error` methods
- **Exceptions:** Create custom exceptions in `exception/` package extending `RuntimeException`
- **DTOs:** Use immutable records or Lombok-annotated classes with builder pattern
- **Braces** - Always required for all conditional statements, loops, and methods. Can be skipped for one-line lambdas

### 3. Naming Conventions
- **Classes/Interfaces:** PascalCase (e.g., `MockServerManager`, `ResponseStrategy`)
- **Methods:** camelCase (e.g., `createServer`, `isTlsEnabled`)
- **Variables:** camelCase (e.g., `serverId`, `tlsConfig`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `DEFAULT_PORT`)
- **Packages:** lowercase with dots (e.g., `io.github.anandb.mockserver.service`)
- **DTOs:** Suffix with `DTO` (e.g., `EnhancedExpectationDTO`)
- **Config classes:** Suffix with `Config` (e.g., `TlsConfig`)
- **Strategy interfaces:** Suffix with `Strategy` (e.g., `ResponseStrategy`)

### 4. MockServer Integration
- Management happens via `MockServerManager`.
- Use `ClientAndServer` from `org.mockserver.integration`.
- For any custom response logic (SSE, Files, Relay, Templates), use the **Strategy Pattern**.
- Register new strategies by implementing `ResponseStrategy` and adding the `@Component` annotation.
- The `EnhancedResponseCallback` automatically picks up all strategies and chooses the best one based on the `EnhancedExpectationDTO`.

## 🤖 AI Instructions

- **Context:** This is a Spring Boot wrapper around Netty MockServer. It manages multiple instances on different ports.
- **Config Formats:** Supports `.json` and `.jsonmc` (JSON with Multiline Comments).
- **Features:** TLS/mTLS, Basic Auth, OAuth2 Relay, SSE, and Multipart file downloads are all handled via a unified `EnhancedExpectationDTO`.
- **Simplification:** Avoid manual JSON manipulation. Always map incoming requests to `EnhancedExpectationDTO`.

