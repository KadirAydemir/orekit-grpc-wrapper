# Orekit Java Coding Standards

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

## Mapper Usage Rules

### Use Mappers for All Conversions

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

### Anti-Pattern: Manual Mapping in Service
```java
// DON'T DO THIS
private GrpcResponse toGrpcResponse(DomainResult result) {
    return GrpcResponse.newBuilder()
            .setField1(result.field1())
            .setField2(result.field2())
            .build();
}
```

### Correct Pattern: Use Mapper
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

## Code Style & Naming

### General Style
- **NO wildcard imports**
- Indent: 4 spaces
- Max method length: 30 lines
- Opening brace on same line
- Always use braces for control structures

### Naming Conventions
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

## Numerical Safety
- Avoid non-deterministic APIs (unordered parallel streams)
- Avoid direct double equality comparisons
- Prefer tolerance-based comparisons
- Time calculations must be explicit about units
```
