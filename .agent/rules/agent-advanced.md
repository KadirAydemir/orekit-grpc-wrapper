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

### API Naming Conventions

#### Standardized Naming for Batch Operations
All batch/streaming endpoints MUST use consistent naming:
- Use `Batch` prefix for batch operations (e.g., `BatchCalculateEclipses`, `BatchGetAccessIntervals`)
- Use `List` suffix for request/response message types only (e.g., `TLEListRequest`, `TLEListResponse`)
- Never mix conventions within the same service

#### Partial Failure Handling
All bulk response messages MUST include an `error` field for partial failure handling:
```protobuf
message BatchResponse {
    // ... response fields ...
    // Error message for partial failures - empty if successful
    string error = 10;
}
```

This allows the service to:
- Return successful results for valid items
- Include error information for failed items without terminating the entire stream
- Provide better client experience with detailed failure information

#### Example Service Definition
```protobuf
service EclipseService {
    // Unary operation
    rpc CalculateEclipses (EclipseRequest) returns (EclipseResponse) {}
    
    // Batch streaming operation with consistent naming
    rpc BatchCalculateEclipses (BatchEclipseRequest) returns (stream EclipseResponse) {}
}

message EclipseResponse {
    int32 norad_id = 1;
    repeated EclipseInterval intervals = 2;
    // Error field for partial failures
    string error = 3;
}
```

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

### Batch Processing Pattern for Batch Operations

When implementing bulk/streaming gRPC operations, use the following pattern for optimal performance:

```java
@Override
public void batchOperation(BatchRequest request, StreamObserver<Response> responseObserver) {
    List<Item> allItems = request.getItemsList();
    log.info("Starting bulk operation for {} items", allItems.size());

    Multi.createFrom().iterable(allItems)
            .group().intoLists().of(200) // Batch size 200-500
            .onItem().transformToUni(chunk -> Uni.createFrom().item(() -> processBatch(chunk, request))
                    .runSubscriptionOn(executor))
            .merge(64) // Parallelism level
            .onItem().transformToMulti(batch -> Multi.createFrom().iterable(batch))
            .merge() // Flatten stream as items finish
            .subscribe().with(
                    responseObserver::onNext,
                    responseObserver::onError,
                    responseObserver::onCompleted
            );
}

// Helper method to process a batch
private List<Response> processChunk(List<Item> chunk, BatchRequest request) {
    List<Response> batchResponses = new ArrayList<>(chunk.size());

    for (Item item : chunk) {
        try {
            // Process individual item
            Result result = service.process(item);
            batchResponses.add(mapper.map(result));
        } catch (Exception e) {
            log.error("Error processing item: {}", item, e);
            // Return partial failure with error field set
            batchResponses.add(Response.newBuilder()
                    .setId(item.getId())
                    .setError(e.getMessage())
                    .build());
        }
    }
    return batchResponses;
}
```

Key principles:
- **Chunking**: Group items into batches (200-500 items) to reduce overhead
- **Parallelism**: Use merge(64) for controlled concurrency with virtual threads
- **Partial Failures**: Catch exceptions at item level and return error in response
- **Streaming**: Release results incrementally rather than blocking until complete
- **Pre-sizing**: Pre-size result lists with `new ArrayList<>(chunk.size())`

### Mapper Usage Rules

#### Use Mappers for All Conversions

All mapping between gRPC and domain objects MUST use MapStruct mappers:

```java
@Mapper(componentModel = "jakarta", ...)
public interface ServiceMapper {
    // Request -> DTO
    DomainRequest toDTO(GrpcRequest source);
    
    // DTO -> Response (REQUIRED - don't manually map in service)
    GrpcResponse map(DomainResult source);
    
    // Nested object mapping
    NestedGrpcObject map(NestedDomainObject source);
    
    // List mapping
    List<NestedGrpcObject> mapList(List<NestedDomainObject> source);
}
```

#### Anti-Pattern: Manual Mapping in Service
```java
// DON'T DO THIS
private GrpcResponse toGrpcResponse(DomainResult result) {
    return GrpcResponse.newBuilder()
            .setField1(result.field1())
            .setField2(result.field2())
            .build();
}
```

#### Correct Pattern: Use Mapper
```java
// CORRECT
@Autowired
ServiceMapper mapper;

public void operation(Request request, StreamObserver<Response> observer) {
    Uni.createFrom().item(() -> service.process(mapper.toDTO(request)))
            .map(mapper::map) // Use mapper for response
            .subscribe().with(...);
}
```

### TLE Parsing Utilities

Use `TleUtils` for all TLE-related parsing operations:

```java
// Extract satellite ID
int satId = TleUtils.extractSatelliteId(line1);

// Validate TLE format
boolean valid = TleUtils.isValidTle(line1, line2);

// Never parse TLE manually in service classes
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

## Mocking Best Practices

### When to Use Mocking

Mock external dependencies to ensure tests remain stable over time:
- **File System Operations**: Use `@TempDir` for temporary files/directories
- **External Services**: Mock gRPC clients, REST APIs, databases
- **Time-Dependent Logic**: Mock `Clock` or use fixed timestamps
- **Configuration Loading**: Mock config properties or use test-specific configs
- **Orekit Data Initialization**: Mock `DataProvidersManager` when testing config classes

### Mocking with Mockito Example

```java
@QuarkusTest
public class OrekitConfigTest {
    
    @Test
    public void init_withMockedDataProvider() {
        // Given - Mock the data provider
        DataProvidersManager mockManager = mock(DataProvidersManager.class);
        
        // When - Test with mocked dependency
        OrekitConfig config = new OrekitConfig();
        config.init();
        
        // Then - Verify interactions
        verify(mockManager).addProvider(any());
    }
}
```

### File System Testing with @TempDir

```java
@QuarkusTest
public class OrekitConfigTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    public void init_withZipFile() throws Exception {
        // Create temporary test resources
        File zipFile = tempDir.resolve("test-data.zip").toFile();
        // ... test logic
    }
}
```

### Mocking Rules

1. **Never mock the class under test** - Test the real implementation
2. **Mock only external dependencies** - Database, file system, network calls
3. **Use @TempDir for file operations** - Automatic cleanup after tests
4. **Mock configuration values** - Use test-specific application.properties
5. **Verify mock interactions** - Ensure expected calls were made
6. **Reset mocks between tests** - Use `@BeforeEach` or `@AfterEach`

### What to Mock in This Project

| Component | Mock Strategy | Reason |
|-----------|---------------|---------|
| OrekitConfig | Use @TempDir | File system dependency |
| External APIs | Mockito mock | Network dependency |
| Database | In-memory/embedded | Persistent state |
| Clock/Time | Fixed timestamps | Deterministic tests |
| Random values | Seeded Random | Reproducible results |

### What NOT to Mock

- **Service implementations** - Test the real logic
- **Domain models** - Simple data containers
- **Internal utilities** - Test actual behavior
- **MapStruct mappers** - Generated code, test through integration

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
