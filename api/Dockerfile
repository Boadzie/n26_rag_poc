FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts ./

# Copy source code
COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/build/libs/rag-api.jar ./app.jar

# Create config directory
RUN mkdir -p /app/config

# Install curl for health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
