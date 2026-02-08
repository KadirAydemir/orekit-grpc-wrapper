# Orekit gRPC Agent Rules

## 🚨 CRITICAL CONSTRAINTS (DO NOT VIOLATE)
1. **Java Version**: Use Java 25 (FINAL features only). No preview features.
2. **Build System**: Use `./mvnw` (Maven Wrapper). Never use `mvn` directly.
3. **No Wildcard Imports**: `import java.util.*;` is FORBIDDEN.
4. **Package Structure**:
   - `tr.com.kadiraydemir.orekit.service.<domain>` (Core Logic)
   - `tr.com.kadiraydemir.orekit.grpc.<domain>` (gRPC Adapters)
   - **Rule**: Service layer classes MUST NOT import gRPC generated classes.
5. **No System.out**: Use structured logging (SLF4J/JBoss Logging).
6. **No Generic Exceptions**: Do not throw `RuntimeException` or `Exception`. Use domain exceptions.
7. **Security**: NEVER log TLE lines or orbital state vectors (potentially classified).
8. **Testing**: All tests must use `@QuarkusTest`. Unit tests are insufficient for native quirks.

## 📚 Rule Index (READ BEFORE ACTING)
Use `view_file` on these files for specific implementation details:

- **[gRPC & Protobuf](grpc-practices.md)**: Proto file rules, API naming, batching patterns.
- **[Java Coding](coding-standards.md)**: Lombok, MapStruct, Code Style, Naming, Injection.
- **[Testing & QA](testing-qa.md)**: Test definitions, mocking, debugging.
- **[Ops & Security](ops-security.md)**: CI/CD, Native Image, Performance, Safety.

## 🛠️ Quick Commands
```bash
./mvnw clean install                    # Build & Test
./mvnw clean install -DskipTests        # Fast Build
./mvnw verify -Pnative                  # Native Integration Test
./mvnw quarkus:dev                      # Dev Mode
```

## ⚠️ Change Policy
- **Minimal Diffs Only**: Do not reformat files unless asked.
- **Documentation**: Update `API_DOC.md` IMMEDIATELY if `.proto` files change.
- **Ambiguity**: STOP and ask if requirements are unclear.

## Development Workflow

### Starting a New Feature
1. Check existing patterns in similar services
2. Define proto first (API contract before implementation)
3. All gRPC service methods must use `@RunOnVirtualThread`
4. Write `@QuarkusTest` before implementation is complete

### Common Patterns
- Single item operation: Return result directly
- Batch operation: Use streaming with error field in response
- Time ranges: Always use `Instant` (protobuf `Timestamp`) with explicit time zones
- Resource cleanup: Use try-with-resources for Orekit propagators
