FROM gradle:latest AS builder
# ^ 2 Critical & 4 High vulnerabilities, as per Docker DX (14.08.2025)
ADD https://github.com/d4rken/octi-sync-server-kotlin.git /octi-k-server
WORKDIR /octi-k-server
RUN ./gradlew clean installDist

FROM openjdk:26-jdk
RUN microdnf install findutils -y
WORKDIR /octi-k-server
COPY --from=builder /octi-k-server/build/install/octi-sync-server-kotlin/ .
ENTRYPOINT ["./bin/octi-sync-server-kotlin", "--datapath=${OCTI_DATAPATH:-/etc/octi-k-server}", "--port=${OCTI_PORT:-8080}"]