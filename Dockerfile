# ============================================
# Multi-stage Dockerfile for Coolify Deployment
# Orekit gRPC Service - GraalVM Native Image
# ============================================

# Stage 1: Build with GraalVM Native Image
FROM ghcr.io/graalvm/native-image-community:25 AS build

USER root
WORKDIR /app

# No need to install manual build tools as they are pre-installed in this image

# Copy Maven wrapper and pom.xml first for dependency caching
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./

# Download dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Copy Orekit data
COPY orekit-data.zip ./

# Build native executable
# -Dnative activates the native profile in pom.xml
RUN ./mvnw package -Dnative -DskipTests -B

# Stage 2: Runtime Image
FROM registry.access.redhat.com/ubi9/ubi:latest

WORKDIR /work

# Create app user and set permissions
# Note: curl is pre-installed in the full ubi9 image, so no microdnf install needed.
RUN chown 1001 /work && \
    chmod "g+rwX" /work && \
    chown 1001:root /work

# Copy Orekit data to the container
COPY --chown=1001:root orekit-data.zip /work/orekit-data.zip

# Copy the native executable from build stage
COPY --chown=1001:root --chmod=0755 --from=build /app/target/*-runner /work/application

# Expose HTTP port (gRPC uses the same port with use-separate-server=false)
EXPOSE 8080

USER 1001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/q/health/ready || exit 1

# Run the native executable
ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]
