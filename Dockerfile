FROM gradle:latest AS builder
# ^ 2 Critical & 4 High vulnerabilities, as per Docker DX (14.08.2025)
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

FROM openjdk:26-jdk

# Install findutils and create non-root user for security
RUN microdnf install findutils -y && \
    microdnf clean all && \
    useradd -r -u 1000 octi-user

WORKDIR /octi-sync-server

# Copy built application
COPY --from=builder /octi-sync-server/build/install/octi-sync-server-kotlin/ .

# Set ownership and make executable
RUN chown -R octi-user:octi-user /octi-sync-server && \
    chmod +x ./bin/octi-sync-server-kotlin

# Create data directory
RUN mkdir -p /etc/octi-sync-server && \
    chown -R octi-user:octi-user /etc/octi-sync-server

# Switch to non-root user
USER octi-user

# Set default environment variables
ENV OCTI_DATAPATH=/etc/octi-sync-server
ENV OCTI_PORT=8080

# Expose port
# EXPOSE 8080
# ^ Why tho?

# Use JSON array format for entrypoint with shell to handle env vars
ENTRYPOINT ["sh", "-c", "./bin/octi-sync-server-kotlin --datapath=${OCTI_DATAPATH} --port=${OCTI_PORT}"]
