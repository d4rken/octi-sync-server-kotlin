#!/bin/bash
#
# Octi Sync Server Entrypoint Script
#
# Environment variables:
# - OCTI_PORT: Server port (default: 8080)
# - OCTI_DEBUG: Enable debug mode (default: false)
#
# Fixed configuration:
# - Data Path: /etc/octi-sync-server
#

set -e

# Process environment variables
OCTI_PORT=${OCTI_PORT:-8080}
OCTI_DEBUG=${OCTI_DEBUG:-false}

# Validate port
if ! [[ "$OCTI_PORT" =~ ^[0-9]+$ ]] || [ "$OCTI_PORT" -lt 1 ] || [ "$OCTI_PORT" -gt 65535 ]; then
    echo "Error: Invalid port number. Must be between 1-65535"
    exit 1
fi

# Build command arguments
CMD_ARGS="--datapath=/etc/octi-sync-server --port=$OCTI_PORT"

# Add debug flag if enabled
if [ "$OCTI_DEBUG" = "true" ]; then
    CMD_ARGS="$CMD_ARGS --debug"
fi

# Execute the application
exec ./bin/octi-sync-server-kotlin $CMD_ARGS
