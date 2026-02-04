# Project: Orekit gRPC Wrapper

## Overview
This is a high-performance gRPC wrapper for the Orekit space flight dynamics library, built with Quarkus and GraalVM.
- **Java Version**: 21+
- **Framework**: Quarkus
- **Build Tool**: Maven (`./mvnw`)

## Architecture & Code Structure
- **Source Code**: `src/main/java/tr/com/kadiraydemir/orekit/grpc`
- **Tests**: `src/test/java/org/acme`
- **Blocking Code**: Blocking computations in Mutiny `Uni` pipelines must use `.runSubscriptionOn(Infrastructure.getDefaultWorkerPool())`.
- **List Optimization**: `ArrayList` resizing optimization is used; initialize with `Math.max(0, count)`.

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
