# Orekit Testing & QA Guidelines

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
