# Code Review Summary

**Scope**: Full codebase review — common Java bugs (null safety, concurrency, resource leaks, logic errors)
**Overall risk**: Medium
**Verdict**: Request changes

## Findings

### [P1] High

- **ConfigurationLoaderService: `else if` silently drops expectations when relays are present**
  - **Location**: `service/ConfigurationLoaderService.java:93-101`
  - **Why it matters**: When a server has both `relays` AND `expectations`, the `else if` causes real expectations to be discarded. A single empty catch-all DTO is configured instead, breaking relay functionality.
  - **Evidence**: `if (!isEmpty(serverInstance.relays())) { ... } else if (config.hasExpectations()) { ... }` — the `else if` means expectations are never processed when relays exist.
  - **Fix**: Change `else if` to a separate `if` block so both relay registration and expectation configuration execute independently.

- **MockServerManager: TOCTOU race on server creation**
  - **Location**: `service/MockServerManager.java:75-109`
  - **Why it matters**: `servers.containsKey(serverId)` check is inside `synchronized`, but `servers.put()` is outside. Two threads can pass the duplicate check and create two servers; the second silently overwrites the first, orphaning it.
  - **Evidence**: Synchronized block ends at line ~85. `servers.put(serverId, instance)` at line ~109 is outside.
  - **Fix**: Move `servers.put()` inside the synchronized block, or use `ConcurrentHashMap.putIfAbsent()`.

- **TlsConfigurationService: Global static `ConfigurationProperties` corrupted by concurrent servers**
  - **Location**: `service/TlsConfigurationService.java:100-126`
  - **Why it matters**: MockServer's `ConfigurationProperties` is global static state. The code only synchronizes the setter calls, not the full write-then-create sequence. Two concurrent TLS server creations overwrite each other's cert paths.
  - **Evidence**: File's own Javadoc acknowledges this. Partial lock at lines 115-118 doesn't prevent interleaving with `ClientAndServer.startClientAndServer()`.
  - **Fix**: This is a fundamental MockServer limitation. Document that TLS servers must be created sequentially, or serialize the entire configure+create sequence.

- **DynamicFileStrategy: NPE when `config.getHttpResponse()` returns null**
  - **Location**: `strategy/DynamicFileStrategy.java:85-86`
  - **Why it matters**: `handleFileResponse()` calls `config.getHttpResponse().getStatusCode()` without null check. If the httpResponse JsonNode deserialization fails or body is empty, `getHttpResponse()` returns null.
  - **Evidence**: `isFileResponse()` only checks `getFile() != null`, not that the response object is valid.
  - **Fix**: Add null check: `if (config.getHttpResponse() == null) return 500 error`.

- **RequestUtils: `Collectors.toMap` NPE on null header values**
  - **Location**: `util/RequestUtils.java:68-74`
  - **Why it matters**: `Collectors.toMap` throws NPE if any value is null. A malformed HTTP header with no value triggers this.
  - **Evidence**: `val.getValue()` can return null; `Collectors.toMap` does not accept null values.
  - **Fix**: Filter null values: `.filter(val -> val.getValue() != null)` before collecting.

- **ConfigGeneratorService: NPE when `outputPath` has no parent directory**
  - **Location**: `service/ConfigGeneratorService.java:102`
  - **Why it matters**: `Files.createDirectories(outPath.getParent())` throws NPE if `outputPath` is a bare filename (e.g., `"config.json"`), because `Path.getParent()` returns null.
  - **Evidence**: `Path.getParent()` returns null for root or single-element paths.
  - **Fix**: Guard with `if (outPath.getParent() != null) Files.createDirectories(outPath.getParent());`

### [P2] Medium

- **EnhancedExpectation: `getFileDisposition()` returns `"null"` string for JSON null**
  - **Location**: `model/EnhancedExpectation.java:87-94`
  - **Why it matters**: When `"fileDisposition": null` is in the JSON, `NullNode.asText()` returns the string `"null"`, not Java null. The method returns `"null"` instead of defaulting to `"attachment"`.
  - **Evidence**: `node.asText(null)` or `node.isNull()` check is needed.
  - **Fix**: Use `fileNode.asText(null)` and treat null as `"attachment"`.

- **FreemarkerTemplateService: `convertValue` throws on non-object JSON bodies**
  - **Location**: `service/FreemarkerTemplateService.java:125`
  - **Why it matters**: If the request body is a JSON array or primitive, `objectMapper.convertValue(body, Map.class)` throws `IllegalArgumentException`.
  - **Evidence**: `body` is a `JsonNode` — could be `ArrayNode`, `TextNode`, etc.
  - **Fix**: Check `context.getBody().isObject()` before converting; fall back to empty map.

- **ResponseUtils: Duplicate header names silently dropped**
  - **Location**: `util/ResponseUtils.java:39-43`
  - **Why it matters**: Converting headers to `Map<NottableString, Header>` with `(h1, h2) -> h1` collapses multiple headers with the same name (e.g., multiple `Set-Cookie` headers) into one.
  - **Evidence**: `Collectors.toMap` with merge function keeps only first entry.
  - **Fix**: Use `Map<NottableString, List<Header>>` or iterate and append individual global headers without collapsing.

- **StaticResponseStrategy: `supports()` doesn't verify non-null response**
  - **Location**: `strategy/StaticResponseStrategy.java:24`
  - **Why it matters**: Returns `true` for any non-SSE, non-file config — even if `getHttpResponse()` would return null. `handle()` then returns null.
  - **Evidence**: `supports()` only checks `!isSse() && !isFileResponse()`.
  - **Fix**: Add `&& config.getHttpResponse() != null` to `supports()`.

- **RelayResponseStrategy: Platform-dependent charset in body relay**
  - **Location**: `strategy/RelayResponseStrategy.java:86`
  - **Why it matters**: `bodyString.getBytes()` uses default platform charset. If remote server expects UTF-8 (HTTP standard), data corruption occurs on non-UTF-8 systems.
  - **Evidence**: No charset parameter passed to `getBytes()`.
  - **Fix**: Use `bodyString.getBytes(StandardCharsets.UTF_8)`.

- **CertificateValidator: `validateCaCertificate()` skips validity period check**
  - **Location**: `service/CertificateValidator.java:110-130`
  - **Why it matters**: Unlike `validateCertificateFormat()` which calls `cert.checkValidity()`, the CA validation never checks if the certificate is expired or not yet valid.
  - **Evidence**: No `checkValidity()` call in `validateCaCertificate()`.
  - **Fix**: Add `cert.checkValidity()` call after parsing.

- **OAuth2TokenService: Race allows redundant token requests**
  - **Location**: `service/OAuth2TokenService.java:58-72`
  - **Why it matters**: Comment claims "atomic check-and-fetch prevents redundant requests" but the fetch is outside the `compute()` call. Multiple threads can simultaneously see expired cache and all fetch new tokens.
  - **Evidence**: `compute()` removes expired entry, returns null. All threads then call `fetchAccessToken()`.
  - **Fix**: Use a `Future`-based cache or synchronize the entire check-and-fetch.

- **ServerInstance: `tunnels()` exposes mutable internal map**
  - **Location**: `model/ServerInstance.java:81-83`
  - **Why it matters**: Returns direct reference to `ConcurrentHashMap`, allowing callers to bypass `addTunnel()`/`removeTunnel()` methods.
  - **Evidence**: `return tunnels;` — no unmodifiable wrapper.
  - **Fix**: Return `Collections.unmodifiableMap(tunnels)`.

- **ErrorCode: `toString()` throws RuntimeException**
  - **Location**: `util/ErrorCode.java:27`
  - **Why it matters**: `toString()` contract says it should never throw. If JSON serialization fails, `RuntimeException` propagates from logging, string concatenation, or debug views.
  - **Evidence**: `throw new RuntimeException(e)` inside `toString()`.
  - **Fix**: Return fallback string on failure: `return "ErrorCode{errorCode='" + errorCode + "', message='" + message + "'}"`.

- **DynamicFileStrategy: Path traversal check uses raw string, not canonical path**
  - **Location**: `strategy/DynamicFileStrategy.java:64-68`
  - **Why it matters**: The `..` check operates on the template-evaluated string, not the resolved canonical path. A symlink bypasses the check.
  - **Evidence**: `normalizedPath.contains("..")` checks the string, but `canonicalPath` is already computed and should be used for directory-prefix validation instead.
  - **Fix**: Verify `canonicalPath.startsWith(expectedBaseDir)` using the canonical base path.

### [P3] Low

- **MockServerManager: `shuttingDown` flag unsynchronized**
  - **Location**: `service/MockServerManager.java:48,259-262`
  - **Why it matters**: Check-and-set between `@PreDestroy` and JVM shutdown hook is not atomic. Both can proceed concurrently.
  - **Fix**: Use `AtomicBoolean.compareAndSet()`.

- **RelayService: Response body not truncated at MAX limit**
  - **Location**: `service/RelayService.java:129-135`
  - **Why it matters**: Reads `MAX + 1` bytes for detection, but returns the oversized body without truncation.
  - **Fix**: Truncate to `MAX_RELAY_BODY_SIZE` when exceeded.

- **SSEResponseStrategy: Duplicate Content-Type header possible**
  - **Location**: `strategy/SSEResponseStrategy.java:47-52`
  - **Why it matters**: SSE headers set `Content-Type: text/event-stream`, then config headers are appended. If config also has Content-Type, response has two conflicting headers.
  - **Fix**: Filter out `Content-Type` from config headers before appending.

- **HttpClientFactory: Endpoint identification null vs empty**
  - **Location**: `util/HttpClientFactory.java:59-61`
  - **Why it matters**: `setEndpointIdentificationAlgorithm(null)` may restore defaults in some JDK implementations instead of disabling verification.
  - **Fix**: Use `setEndpointIdentificationAlgorithm("")` (empty string).

- **Various model classes: Mutable list/map exposure**
  - **Locations**: `ServerCreationRequest.java:76,80`, `ServerConfiguration.java:36`
  - **Why it matters**: Getters return internal list references. Callers can mutate object state.
  - **Fix**: Wrap with `Collections.unmodifiableList()`.

- **ConfigGeneratorService: `TEMPLATE_DESCRIPTIONS` not unmodifiable**
  - **Location**: `service/ConfigGeneratorService.java:32`
  - **Why it matters**: Static map is mutable. `getAvailableTypes()` returns a set backed by the map.
  - **Fix**: Wrap with `Collections.unmodifiableMap()`.

## Suggested Next Steps

- [ ] Fix P1 findings — especially the ConfigurationLoaderService `else if` bug (silent expectation loss) and MockServerManager race condition
- [ ] Add null checks in DynamicFileStrategy and StaticResponseStrategy
- [ ] Fix `getFileDisposition()` to handle JSON null properly
- [ ] Add `StandardCharsets.UTF_8` to RelayResponseStrategy body conversion
- [ ] Add tests for: relay+expectation combo, concurrent server creation, null body template processing
- [ ] Re-run `mvn test` after fixes
