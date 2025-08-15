FROM gradle:latest AS builder
# ^ 2 Critical & 4 High vulnerabilities, as per Docker DX (14.08.2025)
ADD https://github.com/d4rken/octi-sync-server-kotlin.git /octi-sync-server
WORKDIR /octi-sync-server
RUN ./gradlew clean installDist

FROM openjdk:26-jdk
RUN microdnf install findutils -y
WORKDIR /octi-sync-server
COPY --from=builder /octi-sync-server/build/install/octi-sync-server-kotlin/ .

# Set default environment variables
ENV OCTI_DATAPATH=/etc/octi-sync-server
ENV OCTI_PORT=8080

# Use JSON array format for entrypoint with shell to handle env vars
ENTRYPOINT ["sh", "-c", "./bin/octi-sync-server-kotlin --datapath=${OCTI_DATAPATH} --port=${OCTI_PORT}"]
