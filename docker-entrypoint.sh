#!/bin/bash
#
# Octi Sync Server Entrypoint Script
#
# Environment variables:
# - OCTI_DEBUG: Enable debug mode (default: false)
#
# Fixed configuration:
# - Data Path: /etc/octi-sync-server
# - Port: 8080
#

set -e

# Process environment variables
OCTI_DEBUG=${OCTI_DEBUG:-false}

# Fixed arguments
CMD_ARGS="--datapath=/etc/ocit-sync-server --port=8080"

# Add debug flag if enabled
if [ "$OCTI_DEBUG" = "true" ]; then
    CMD_ARGS="$CMD_ARGS --debug"
fi

# Execute the application
exec ./bin/octi-sync-server-kotlin $CMD_ARGS
