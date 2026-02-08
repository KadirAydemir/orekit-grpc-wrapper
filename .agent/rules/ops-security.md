# Orekit Operations & Security

## CI/CD Commands

### Full Pipeline
```bash
#!/bin/bash
set -e

# Clean build with tests
./mvnw clean verify

# Generate coverage report
./mvnw jacoco:report

# Build native image for deployment
./mvnw package -Dnative -DskipTests

# Or build JVM uber-jar
./mvnw package -Dquarkus.package.type=uber-jar
```

### Quality Checks
```bash
# Check for dependency updates
./mvnw versions:display-dependency-updates

# Analyze dependencies
./mvnw dependency:analyze

# Check for vulnerabilities
./mvnw org.owasp:dependency-check-maven:check
```

## Performance Optimization

### List Pre-sizing
```java
// Good - pre-size the list
int expectedSize = calculateExpectedSize();
List<Result> results = new ArrayList<>(Math.max(0, expectedSize));

// Bad - causes resizing
List<Result> results = new ArrayList<>();  // default size 10
```

### Stream Optimization
```java
// Good - explicit iteration order
List<Result> sorted = items.stream()
    .sorted(Comparator.comparing(Item::getTimestamp))
    .collect(Collectors.toList());

// Bad - unordered parallel stream (non-deterministic)
List<Result> bad = items.parallelStream()  // Don't use for orbital calculations
    .map(this::calculate)
    .collect(Collectors.toList());
```

### Virtual Threads
```java
// Good - run blocking operations on virtual threads
@RunOnVirtualThread
public void blockingOperation() {
    // This runs on a virtual thread
    Thread.sleep(1000);
}

// Good - use custom executor for parallel operations
@Inject
@Named("propagationExecutor")
ExecutorService executor;

public Multi<Result> parallelProcess(List<Request> requests) {
    return Multi.createFrom().iterable(requests)
        .emitOn(executor)
        .map(this::process);
}
```

## Health Checks

### Liveness Check (is app running?)
```java
@Liveness
@ApplicationScoped
public class MyLivenessCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("liveness");
    }
}
```

### Readiness Check (is app ready to serve?)
```java
@Readiness
@ApplicationScoped
public class OrekitDataReadyCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        // Check if orekit-data is loaded
        boolean dataLoaded = checkOrekitData();
        return HealthCheckResponse.builder()
            .name("orekit-data")
            .status(dataLoaded)
            .build();
    }
}
```

### Health Check Injection
```java
@Inject
@Liveness
HealthCheck livenessCheck;

@Inject
@Readiness
HealthCheck readinessCheck;
```

## Security Considerations

### Data Protection
- Never log TLE lines in production (may contain classified satellites)
- Validate all user inputs before processing
- Use rate limiting for expensive operations
- Sanitize error messages (don't expose internal details)

### Example Safe Logging
```java
// Bad - logs potentially classified data
log.info("Processing TLE: {}", tleLine);  // DON'T DO THIS

// Good - log only metadata
log.info("Processing satellite: {}, epoch: {}", 
    satelliteName, 
    epoch);
```

## CI/CD Pipeline

### Recommended Workflow
```yaml
stages:
  1. Build & Test        # ./mvnw clean compile test
  2. Integration Tests   # ./mvnw verify
  3. Code Quality        # Checkstyle, SpotBugs
  4. Security Scan       # OWASP dependency check
  5. Native Image        # ./mvnw package -Dnative
  6. Deploy
```

### Fast Feedback Loop
```bash
# Pre-commit (fast)
./mvnw spotless:check

# Before push (medium)
./mvnw clean test

# Before merge (thorough)
./mvnw clean verify

# Nightly (slow)
./mvnw verify -Pnative
./mvnw dependency-check
```

## Performance Budgets

### Response Time Targets
| Operation | Target P95 | Target P99 |
|-----------|-----------|-----------|
| Single TLE propagation | < 100ms | < 200ms |
| Batch propagation (100) | < 2s | < 5s |
| Eclipse calculation | < 500ms | < 1s |
| Access windows | < 1s | < 3s |

### Memory Guidelines
```java
// Good - Bounded cache
@CacheResult(cacheName = "tle-cache")
@CacheInvalidateAll(cacheName = "tle-cache")
public TLE getTLE(String id) { }

// Good - Pre-size collections
List<Result> results = new ArrayList<>(expectedSize);

// Bad - Unbounded cache
private static final Map<String, TLE> CACHE = new HashMap<>();
```

## Native Image Optimization

### Reflection Configuration
```json
[
  {
    "name": "tr.com.kadiraydemir.orekit.service.propagation.PropagationService",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true
  }
]
```

### Use @RegisterForReflection
```java
@RegisterForReflection
public class OrbitalState {
    private double x, y, z;
}
```

## Health Check Strategy

### Layered Health Checks
```java
// Liveness - Process running
@Liveness
public HealthCheckResponse call() { return up("process"); }

// Readiness - Can serve traffic
@Readiness
public HealthCheckResponse call() { 
    return builder().name("orekit-data").status(ready).build(); 
}

// Startup - Init complete
@Startup
public HealthCheckResponse call() { 
    return builder().name("cache-warmup").status(warmed).build(); 
}
```

## Security Checklist

- [ ] No hardcoded secrets (use `@ConfigProperty`)
- [ ] TLE data not logged (use names/IDs only)
- [ ] Input validation on all gRPC endpoints
- [ ] Rate limiting for expensive operations
- [ ] Error messages don't leak stack traces
- [ ] Dependencies scanned (`./mvnw dependency-check`)

## Resilience Patterns

### Graceful Degradation
```java
@Fallback(fallbackMethod = "propagateWithSimplifiedModel")
@Timeout(value = 5, unit = TimeUnit.SECONDS)
public PropagationResult propagate(PropagationRequest request) {
    return highFidelityPropagator.propagate(request);
}

public PropagationResult propagateWithSimplifiedModel(PropagationRequest request) {
    log.warn("Falling back to simplified model");
    return simplifiedPropagator.propagate(request);
}
```

### Circuit Breaker
```java
@CircuitBreaker(
    requestVolumeThreshold = 10,
    failureRatio = 0.5,
    delay = 10000
)
public ExternalTle fetchFromExternalSource(String id) {
    return externalClient.fetch(id);
}
```
