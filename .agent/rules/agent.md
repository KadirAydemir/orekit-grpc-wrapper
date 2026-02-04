# Project: Orekit gRPC Wrapper

## Overview
This is a high-performance gRPC wrapper for the Orekit space flight dynamics library, built with Quarkus and GraalVM.
- **Java Version**: 25
- **Framework**: Quarkus
- **Build Tool**: Maven (`./mvnw`)

## Architecture & Code Structure
- **Source Code**: `src/main/java/tr/com/kadiraydemir/orekit/grpc`
- **Tests**: `src/test/java/tr/com/kadiraydemir/orekit/grpc`
- **Service Package Structure**: Maintain a modular structure under `tr.com.kadiraydemir.orekit.service` by grouping related services into subpackages (e.g., `propagation`, `visibility`, `transformation`, `frame`). Avoid dumping all classes directly into the `service` package.
- **gRPC Package Structure**: Similar to services, grouping gRPC implementations in `tr.com.kadiraydemir.orekit.grpc` into subpackages (e.g., `propagation`, `visibility`).
- **Test Package Structure**: Test classes should mirror the source package structure. For example, tests for `propagation` services should be in `tr.com.kadiraydemir.orekit.grpc.propagation` or `tr.com.kadiraydemir.orekit.service.propagation` depending on what they test.
- **Blocking Code**: Blocking computations should leverage Java 21 Virtual Threads (e.g., using `@RunOnVirtualThread` or `runSubscriptionOn(Infrastructure.getDefaultWorkerPool())` only if virtual threads are not applicable).
- **List Optimization**: `ArrayList` resizing optimization is used; initialize with `Math.max(0, count)`.
- **Domain Models**: Services must use domain models (POJOs) for return types and business logic interactions. Do NOT use gRPC generated classes directly in the service layer's return types; restrict them to the gRPC service endpoints (`*GrpcService`) for mapping.

## Token Economy & File Constraints (CRITICAL)
To minimize token usage and avoid wasting context window space:

1.  **IGNORED FILES**: Do NOT read or list contents of the following files/directories:
    - `orekit-data.zip` (Large binary data)
    - `target/` (Build artifacts)
    - `.mvn/` (Maven wrapper internals)
    - `*.jar`, `*.class`, `*.zip`, `*.geoid`
    - `mvnw`, `mvnw.cmd` (unless debugging the wrapper itself)
    - `.git/`

2.  **Efficient Exploration**:
    - Use `ls -F` or `list_files` on specific subdirectories rather than listing the entire root recursively if not needed.
    - Read only relevant source files (`.java`, `pom.xml`, config files).

## Commands
- **Build**: `./mvnw clean install`
- **Test**: `./mvnw test`
- **Code Style**: Always use imports instead of fully qualified names to keep the code clean and readable, especially for project classes (e.g., `tr.com.kadiraydemir.orekit.grpc.*`).
