FROM gradle:latest AS builder
# ^ 2 Critical & 4 High vulnerabilities (14.08.2025)
WORKDIR /octi-sync-server

# Copy Gradle wrapper files first for better caching
COPY gradlew ./
COPY gradlew.bat ./
COPY gradle/ ./gradle/

# Convert line endings and make gradlew executable (fixes Windows CRLF issues)
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

# Copy build files for dependency resolution
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY gradle.properties ./

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ ./src/

# Build the application
RUN ./gradlew clean installDist --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /octi-sync-server

# Create non-root user for security
RUN useradd -r -u 1000 octi-user

# Copy built application
COPY --from=builder /octi-sync-server/build/install/octi-sync-server-kotlin/ .

# Copy entrypoint script
COPY docker-entrypoint.sh .

# Fix line endings and set ownership and make executable
RUN sed -i 's/\r$//' ./docker-entrypoint.sh && \
    chown -R octi-user:octi-user /octi-sync-server && \
    chmod +x ./bin/octi-sync-server-kotlin && \
    chmod +x ./docker-entrypoint.sh

# Switch to non-root user
USER octi-user

# Use the entrypoint script
ENTRYPOINT ["./docker-entrypoint.sh"]
