# Orekit gRPC Wrapper - Essential Guidelines

Quick reference for agentic coding agents. For advanced topics, see `agent-advanced.md`.

## Build Commands

```bash
./mvnw clean install                    # Build and install
./mvnw test                             # Run all tests
./mvnw test -Dtest=ClassName            # Single test class
./mvnw test -Dtest=ClassName#method     # Single test method
./mvnw clean install -DskipTests        # Skip tests
./mvnw verify -Pnative                  # Integration tests
./mvnw quarkus:dev                      # Development mode
./mvnw package -Dnative                 # Native image
./mvnw package -Dquarkus.package.type=uber-jar  # Uber-jar
./mvnw jacoco:report                    # Coverage report
```

## Critical Rules

### Java & Build
- Java 25 (FINAL features only)
- Preview/incubator features FORBIDDEN
- Use `./mvnw` (Maven wrapper)

### Code Style
- NO wildcard imports
- Indent: 4 spaces
- Max method length: 30 lines
- Opening brace on same line
- Always use braces for control structures

### Naming
- Classes: `PascalCase`
- Methods/variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: lowercase

### Types & Data Modeling
- Use `record` for: DTOs, API request/response objects, immutable values
- Use `class` for: domain models with behavior, stateful objects
- NO Lombok for DTOs if record is applicable
- DTOs must NOT have 'Dto' or 'DTO' suffix
- Explicit types in public APIs (no `var` in signatures)

### Dependency Injection
```java
// Prefer package-private field injection (required for JaCoCo)
@Inject
FrameService frameService;

// Constructor injection ONLY if explicitly requested
```

### Architecture
- Service layer: `tr.com.kadiraydemir.orekit.service.<domain>`
- gRPC layer: `tr.com.kadiraydemir.orekit.grpc.<domain>`
- Service layer MUST NOT expose gRPC generated classes
- Tests must mirror source package structure exactly
- NO business logic in gRPC layer

### Error Handling
```java
// Prefer domain-specific unchecked exceptions
throw new OrekitException("Propagation failed: " + e.getMessage(), e);

// NEVER throw generic RuntimeException
// gRPC status mapping happens ONLY in gRPC layer
```

### Concurrency
- Blocking operations MUST run on virtual threads (`@RunOnVirtualThread`)
- Optimize lists: `new ArrayList<>(Math.max(0, expectedSize))`

### Testing
- Use JUnit 5 with `@QuarkusTest`
- Extended timeouts: `Duration.ofSeconds(30)`
- Health checks: `@Inject @Liveness`
- Prefer integration tests over mocking
- Use `@Timeout` and `@DisplayName` annotations

### Numerical Safety
- Avoid non-deterministic APIs (unordered parallel streams)
- Avoid direct double equality comparisons
- Prefer tolerance-based comparisons
- Time calculations must be explicit about units

### Logging
- Use structured logging
- Do NOT log large objects or Orekit internal state
- Never log TLE lines (may contain classified data)
- Avoid logging in tight loops

### GraalVM Native Image
- Avoid reflection unless required
- Avoid dynamic classloading
- Keep initialization deterministic

## Change Rules (CRITICAL)

- Modify only what is necessary
- Do NOT refactor unless explicitly asked
- STOP and ask if change impacts multiple packages
- STOP and ask if requirements are ambiguous
- STOP and ask before introducing new dependencies
- Do NOT rewrite entire classes for small changes
- Minimal diffs only

## Quick Reference

**Proto files:** `src/main/proto/`  
**Config:** `application.properties`  
**Health endpoints:** `/q/health`, `/q/health/live`, `/q/health/ready`

For detailed guidelines on Protobuf, Lombok, Health Checks, Configuration, and CI/CD, see `agent-advanced.md`.
