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
