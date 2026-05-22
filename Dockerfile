# Multi-stage build for Spring Boot Java application
# Build stage: Eclipse Temurin JDK 21 (slim base for smaller build footprint)
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Copy source code
COPY src ./src

# Build the application
# Ensure the Maven wrapper is executable in the Linux container
# Using -DskipTests to skip tests during build for faster deployment
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests -q

# Runtime stage: Eclipse Temurin JRE 21-alpine (minimal runtime image)
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks (Render requires a way to check health)
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/kumbukaa-*.jar app.jar

# Switch to non-root user
USER appuser

# Expose port (Render will map this)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f https://kumbukaa-app.onrender.com/kumbukaa/health || exit 1

# Start the application with JVM optimizations for containerized environments
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:MinRAMPercentage=50.0", \
    "-Dserver.port=8080", \
    "-jar", \
    "app.jar"]
