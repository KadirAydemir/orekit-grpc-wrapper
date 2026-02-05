# Project: Orekit gRPC Wrapper

## Overview
High-performance gRPC wrapper for the Orekit space flight dynamics library.
Built with Quarkus and GraalVM Native Image.

- Java Version: 25 (FINAL features only)
- Framework: Quarkus
- Build Tool: Maven (./mvnw)

IMPORTANT:
- maven.compiler.release MUST be 25
- Preview or incubator features are FORBIDDEN
- If a feature requires --enable-preview, it must NOT be used

---

## Architecture & Code Structure

Source Root:
- src/main/java/tr/com/kadiraydemir/orekit/grpc

Packages:
- Service layer:
  tr.com.kadiraydemir.orekit.service.<domain>
  (propagation, visibility, transformation, frame, etc.)

- gRPC layer:
  tr.com.kadiraydemir.orekit.grpc.<domain>

- Tests:
  Must mirror source package structure exactly

Rules:
- Do NOT dump classes into root service or grpc packages
- Keep domain boundaries explicit

---

## Data Modeling Rules

- Use record for:
  - DTOs
  - API request / response objects
  - gRPC mapping targets
  - Immutable value objects

- Use class for:
  - Domain models with behavior
  - Stateful or mutable lifecycle objects

- Records:
  - May define compact constructors for validation
  - Must remain immutable

- Do NOT use Lombok for DTOs if record is applicable
- DTOs must NOT have a 'Dto' or 'DTO' suffix

---

## gRPC & Service Layer Rules

- Service layer MUST NOT expose gRPC generated classes
- gRPC classes (*GrpcService) are mapping layers only
- No business logic in gRPC layer

---

## Mapping Rules

- gRPC ↔ domain mapping must be explicit and isolated
- Mapping code must be side-effect free
- No validation or business logic inside mappers

---

## Concurrency & Performance

- Blocking operations MUST run on virtual threads
  - Prefer @RunOnVirtualThread
  - Avoid worker pools unless virtual threads are impossible

- Use non-blocking stubs to parallelize RPC calls
- Provide custom executors with bounded thread limits

- Optimize lists:
  - new ArrayList<>(Math.max(0, expectedSize))

---

## GraalVM Native Image Rules

- Avoid reflection unless explicitly required
- Avoid dynamic classloading
- Keep initialization deterministic
- No runtime scanning assumptions

---

## Determinism & Numerical Safety

- Avoid non-deterministic APIs (e.g., unordered parallel streams)
- Explicitly define iteration order when order matters
- Avoid direct double equality comparisons
- Prefer tolerance-based comparisons
- Time calculations must be explicit about units
- Assume IEEE-754 semantics

---

## Dependency Injection (Quarkus-specific)

- Prefer package-private field injection:
  @Inject MyService service;

Reason:
- Required for stable JaCoCo coverage with Quarkus bytecode generation

- Constructor injection ONLY if explicitly requested

---

## Testing Rules

- Use JUnit 5
- Use extended timeouts for Orekit initialization:
  Duration.ofSeconds(30) or more if required

- Health checks:
  Always inject with explicit qualifier:
  @Inject @Liveness

- Prefer integration tests over excessive mocking

---

## Error Handling Rules

- Prefer domain-specific unchecked exceptions
- Do NOT throw generic RuntimeException
- gRPC status mapping happens ONLY in gRPC layer

---

## Logging Rules

- Use structured logging where possible
- Do NOT log large objects or Orekit internal state
- Avoid logging inside tight loops

---

## API Evolution Rules

- Do NOT introduce breaking changes without explicit request
- New fields in DTOs must be backward-compatible
- gRPC contract changes require justification

---

## Coding Standards

- Prefer immutability
- No wildcard imports
- Explicit types in public APIs
- Method length <= 30 lines unless justified
- Minimal diffs only

---

## Change Scope & Failsafe Rules (CRITICAL)

- Modify only what is necessary for the requested task
- Do NOT refactor unless explicitly asked
- If a change impacts multiple packages, STOP and ask
- If requirements are ambiguous, STOP and ask
- Do NOT guess orbital mechanics behavior

---

## Agent Behavior Rules

- Do NOT introduce new libraries without approval
- Do NOT rewrite entire classes for small changes
- Explain reasoning briefly and concisely

---

## Token Economy & File Constraints (CRITICAL)

IGNORED FILES / DIRECTORIES:
- orekit-data.zip
- target/
- .mvn/
- .git/
- *.jar, *.class, *.zip, *.geoid
- mvnw, mvnw.cmd (unless wrapper debugging)

Exploration Rules:
- Never scan entire repository
- List only specific directories when needed
- Read only relevant .java, pom.xml, and config files

---

## Commands

- Build: ./mvnw clean install
- Test: ./mvnw test

Code Style:
- Always use imports (no fully qualified names in code)
