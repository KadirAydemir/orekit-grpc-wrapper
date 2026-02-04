# Orekit gRPC Wrapper

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This project is a high-performance gRPC wrapper around [Orekit](https://www.orekit.org/), the leading open-source space flight dynamics library.
Built with [Quarkus](https://quarkus.io/), it provides a native (or JVM) microservice for orbital mechanics calculations.

## Features

- **Orbit Propagation**: Propagate satellite orbits using SGP4/SDP4, Keplerian, or Numerical propagators.
- **Coordinate Transformations**: Handles frame conversions (GCRF, EME2000, ITRF, TEME, etc.) automatically.
- **gRPC Interface**: Strongly typed, efficient API for cross-language integration (Python, Go, Node, etc.).
- **Health Checks**: Built-in liveness and readiness probes.

## Prerequisites

- JDK 25+
- Maven 3.9+ (Wrapper included)
- `orekit-data.zip`: Required for physical data (leap seconds, EOP). This repository expects `orekit-data.zip` in the project root.

## Getting Started

### 1. Run in Development Mode

```bash
./mvnw quarkus:dev
```

The gRPC server will listen on `localhost:8080` (unified HTTP server).

### 2. Run Tests

```bash
./mvnw test
```

## API Definition

The service is defined in `src/main/proto/orbital_service.proto`.

```protobuf
service OrbitalService {
  rpc Propagate (PropagateRequest) returns (PropagateResponse) {}
  rpc PropagateTLE (TLEPropagateRequest) returns (TLEPropagateResponse) {}
}
```

## Building

### JVM Mode (Uber-Jar)

```bash
./mvnw package -Dquarkus.package.type=uber-jar
java -jar target/orekit-grpc-wrapper-1.0.0-SNAPSHOT-runner.jar
```

### Docker Build (GraalVM Native Image) ⚡

The default Dockerfile uses **GraalVM Native Image** for optimal performance:

```bash
docker build -t orekit-grpc-wrapper .
docker run -p 8080:8080 orekit-grpc-wrapper
```

**Benefits of Native Image:**
- 🚀 **Startup time**: ~50ms (vs 2-5s with JVM)
- 💾 **Memory usage**: 50-70% less than JVM
- 📦 **Container size**: Smaller runtime image

> ⚠️ **Note**: Native compilation takes 5-10 minutes and requires significant memory (4GB+ recommended).

### Local Native Executable

Requires [GraalVM 25+](https://www.graalvm.org/downloads/) installed locally:

```bash
./mvnw package -Dnative
./target/orekit-grpc-wrapper-1.0.0-SNAPSHOT-runner
```

## Deployment

### Coolify Deployment

This project is configured for easy deployment with [Coolify](https://coolify.io/).

1. **Create a new project** in Coolify
2. **Add a new resource** → Select "Docker"
3. **Connect your Git repository** (GitHub/GitLab)
4. **Configure the deployment:**
   - **Build Pack**: Docker
   - **Dockerfile Location**: `./Dockerfile` (auto-detected)
   - **Port**: `8080`
5. **Set your domain**: e.g., `orekit-api.yourdomain.com`
6. **Deploy!**

#### Environment Variables (Optional)

| Variable | Description | Default |
|----------|-------------|---------|
| `JAVA_OPTS_APPEND` | Additional JVM options | - |
| `JAVA_MAX_MEM_RATIO` | Max heap as % of container memory | `50` |

#### Health Check Endpoints

- **Liveness**: `GET /q/health/live`
- **Readiness**: `GET /q/health/ready`
- **Full Health**: `GET /q/health`

## Endpoints

| Endpoint | Protocol | Description |
|----------|----------|-------------|
| `/` | HTTP/2 (gRPC) | gRPC service endpoint |
| `/q/health` | HTTP | Health check |
| `/q/health/live` | HTTP | Liveness probe |
| `/q/health/ready` | HTTP | Readiness probe |

## License

Apache-2.0

