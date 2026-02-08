# Orekit gRPC Guidelines

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

#### Documentation Maintenance
Whenever a `.proto` file is created or modified, you MUST:
1. Update `API_DOC.md` to reflect the changes (new services, methods, messages).
2. Ensure updated example JSON payloads are provided.
3. Update the "Appendix: Protobuf Definitions" in `API_DOC.md` with the full content of the new/modified `.proto` file.

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

### TLE Parsing Utilities

Use `TleUtils` for all TLE-related parsing operations:

```java
// Extract satellite ID
int satId = TleUtils.extractSatelliteId(line1);

// Validate TLE format
boolean valid = TleUtils.isValidTle(line1, line2);

// Never parse TLE manually in service classes
```

## Proto Design Guidelines

### Field Numbering
```protobuf
// Ranges: 1-10 (IDs), 11-20 (params), 21-30 (config), 90-99 (metadata)
message PropagateRequest {
  string satellite_id = 1;
  double start_time_jd = 11;
  bool include_velocity = 21;
  string request_id = 90;
}
```

### Error Handling
```java
// INVALID_ARGUMENT - bad TLE, invalid date range
// NOT_FOUND - unknown satellite ID
// INTERNAL - Orekit initialization failed
// RESOURCE_EXHAUSTED - rate limiting
throw Status.NOT_FOUND.withDescription("Satellite not found").asRuntimeException();
```

### gRPC Patterns

| Pattern | Use When | Example |
|---------|----------|---------|
| Unary | Single request/response | `GetSatelliteInfo` |
| Server Streaming | Large response | `BatchCalculateEclipses` |
| Client Streaming | Large request | `UploadTLECatalog` |
| Bidirectional | Real-time stream | `LiveTrackingStream` |

### Performance Guidelines

| Operation | Optimal Batch Size |
|-----------|-------------------|
| TLE propagation | 200-500 items |
| Eclipse calculation | 50-100 items |
| Access calculations | 100-200 items |
| Database operations | 500-1000 items |

### Security
- Never log protos containing orbital data
- Sanitize satellite names in error messages
- Validate all IDs before database lookups
- Rate limit expensive operations
