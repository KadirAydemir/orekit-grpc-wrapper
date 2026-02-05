# Orekit gRPC Wrapper - Advanced Guidelines

Detailed best practices for complex scenarios. For essential rules, see `agent.md`.

## Protobuf & gRPC Best Practices

### File Organization
```
src/main/proto/
├── v1/
│   └── orbital_service.proto
└── common/
    └── types.proto
```

### Proto Options
```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "tr.com.kadiraydemir.orekit.grpc";
option java_outer_classname = "OrbitalServiceProto";

service OrbitalService {
  // Document each RPC method
  rpc Propagate (PropagateRequest) returns (PropagateResponse);
}

message PropagateRequest {
  // Document fields - becomes JavaDoc
  string satellite_name = 1;
  double semimajor_axis = 2;
}
```

### gRPC Layer Rules
- gRPC services extend `*Grpc.*ImplBase`
- Map gRPC requests to domain objects immediately on entry
- Map domain results to gRPC responses at exit boundary
- Never expose proto-generated classes in service layer
- Use `@GrpcService` and `@RunOnVirtualThread` annotations

## Lombok Usage Guidelines

### When to Use
| Use Case | Solution |
|----------|----------|
| Service logging | `@Slf4j` or standard Logger |
| Service constructors | `@RequiredArgsConstructor` |
| DTOs/Requests | Use `record` instead |
| Entities | Avoid `@Data`, use explicit getters/setters |

### Logging Setup
```java
// Option 1: Lombok (requires lombok.config)
@Slf4j
public class MyService {
    public void doSomething() {
        log.info("Doing something");
    }
}

// Option 2: Standard SLF4J (recommended, more reliable)
public class MyService {
    private static final Logger log = LoggerFactory.getLogger(MyService.class);
    
    public void doSomething() {
        log.info("Doing something");
    }
}
```

### Lombok Configuration
Create `lombok.config` in project root:
```properties
config.stopBubbling = true
lombok.addLombokGeneratedAnnotation = true
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

## Configuration Management

### Application Properties
```properties
# src/main/resources/application.properties
quarkus.http.port=8080
orekit.data.path=./orekit-data
propagation.thread-pool.size=10
```

### Profile-Specific Configs
```properties
# src/main/resources/application-dev.properties
quarkus.log.level=DEBUG

# src/main/resources/application-prod.properties
quarkus.log.level=INFO
```

### Configuration Injection
```java
@ApplicationScoped
public class PropagationConfig {
    @ConfigProperty(name = "orekit.data.path", defaultValue = "./orekit-data")
    String orekitDataPath;
    
    @ConfigProperty(name = "propagation.thread-pool.size", defaultValue = "10")
    int threadPoolSize;
}
```

### Named Qualifiers
```java
// Define multiple beans
@Produces
@Named("propagationExecutor")
public ExecutorService propagationExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}

@Produces
@Named("transformationExecutor")
public ExecutorService transformationExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}

// Inject specific bean
@Inject
@Named("propagationExecutor")
ExecutorService propagationExecutor;
```

## Testing Patterns

### Test Structure
```
src/test/java/
tr/com/kadiraydemir/orekit/
├── service/
│   └── propagation/
│       └── PropagationServiceImplTest.java
├── grpc/
│   └── propagation/
│       └── PropagationGrpcServiceTest.java
└── mapper/
    └── PropagationTestMapper.java
```

### Comprehensive Test Example
```java
@QuarkusTest
@DisplayName("Propagation Service Tests")
public class PropagationServiceImplTest {

    @Inject
    PropagationService propagationService;
    
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Should propagate TLE and return valid results")
    public void testPropagateTLE() {
        // Given
        TLEPropagateRequest request = createRequest();
        
        // When
        List<TleResult> results = propagationService
            .propagateTLE(request)
            .collect().asList()
            .await().atMost(Duration.ofSeconds(5));
        
        // Then
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }
}
```

### Test Data Mappers
```java
@ApplicationScoped
public class PropagationTestMapper {
    
    public PropagateRequest toDTO(tr.com.kadiraydemir.orekit.grpc.PropagateRequest grpcRequest) {
        return new PropagateRequest(
            grpcRequest.getSatelliteName(),
            grpcRequest.getSemimajorAxis(),
            // ... map all fields
        );
    }
}
```

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

## Debugging Tips

### Enable Debug Logging
```properties
# application-dev.properties
quarkus.log.level=DEBUG
quarkus.log.category."tr.com.kadiraydemir.orekit".level=DEBUG
```

### Thread Dump
```bash
# Get thread dump for debugging
jstack <pid> > thread-dump.txt
```

### Memory Analysis
```bash
# Generate heap dump
jmap -dump:format=b,file=heap.hprof <pid>

# Analyze with Eclipse MAT or VisualVM
```

## Troubleshooting Common Issues

### Lombok Not Working
1. Check `lombok.config` exists
2. Verify annotation processor is enabled in IDE
3. Use standard Logger as fallback

### Native Image Build Fails
1. Check for reflection usage
2. Add reflection config: `META-INF/native-image/reflect-config.json`
3. Use `@RegisterForReflection` on classes that need reflection

### Tests Timeout
1. Increase timeout: `@Timeout(value = 60, unit = TimeUnit.SECONDS)`
2. Check Orekit data initialization
3. Verify virtual threads are working

### JaCoCo Coverage Issues
1. Ensure package-private field injection (not constructor)
2. Run with: `./mvnw clean verify`
3. Check report: `target/site/jacoco/index.html`
