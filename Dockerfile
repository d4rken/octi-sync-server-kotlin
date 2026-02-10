FROM gradle:8.13 AS builder
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

FROM eclipse-temurin:24-jre
# ^ 3 Medium, 2 Low vulnerabilities (04.12.2025)
WORKDIR /octi-sync-server

# Create non-root user for security (let system assign UID)
RUN useradd -r -s /bin/bash -m octi-user

# Copy built application
COPY --from=builder /octi-sync-server/build/install/octi-sync-server-kotlin/ .

# Copy entrypoint script
COPY docker-entrypoint.sh .

# Create data directory and set permissions
RUN mkdir -p /etc/octi-sync-server && \
    sed -i 's/\r$//' ./docker-entrypoint.sh && \
    chown -R octi-user:octi-user /octi-sync-server /etc/octi-sync-server && \
    chmod +x ./bin/octi-sync-server-kotlin && \
    chmod +x ./docker-entrypoint.sh

# Switch to non-root user
USER octi-user

# Expose the application port
EXPOSE 8080

# Declare volume for data persistence
VOLUME ["/etc/octi-sync-server"]

# Use the entrypoint script
ENTRYPOINT ["./docker-entrypoint.sh"]
