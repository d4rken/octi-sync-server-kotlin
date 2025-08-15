#!/bin/bash
#
# Octi Sync Server Entrypoint Script
#
# Environment variables:
# - OCTI_DATAPATH: Path to store server data (default: /etc/octi-sync-server)
# - OCTI_PORT: Server port (default: 8080)
# - OCTI_DEBUG: Enable debug mode (default: false)
#
# The script will:
# 1. Validate environment variables
# 2. Create data directory if it doesn't exist
# 3. Check directory permissions
# 4. Start the application with appropriate flags
#

set -e

# Default values
DEFAULT_PORT=8080
DEFAULT_DATAPATH="/etc/octi-sync-server"

# Process environment variables
OCTI_PORT=${OCTI_PORT:-$DEFAULT_PORT}
OCTI_DATAPATH=${OCTI_DATAPATH:-$DEFAULT_DATAPATH}
OCTI_DEBUG=${OCTI_DEBUG:-false}

# Validation functions
validate_port() {
    if ! [[ "$OCTI_PORT" =~ ^[0-9]+$ ]] || [ "$OCTI_PORT" -lt 1024 ] || [ "$OCTI_PORT" -gt 65535 ]; then
        echo "Error: Invalid port number. Must be between 1024-65535"
        exit 1
    fi
}

validate_datapath() {
    if [ ! -d "$OCTI_DATAPATH" ]; then
        echo "Creating data directory: $OCTI_DATAPATH"
        mkdir -p "$OCTI_DATAPATH"
    fi
    
    if [ ! -w "$OCTI_DATAPATH" ]; then
        echo "Error: Data directory is not writable: $OCTI_DATAPATH"
        exit 1
    fi
}

# Validate inputs
validate_port
validate_datapath

# Build command arguments
CMD_ARGS="--datapath=$OCTI_DATAPATH --port=$OCTI_PORT"

# Add debug flag if enabled
if [ "$OCTI_DEBUG" = "true" ]; then
    CMD_ARGS="$CMD_ARGS --debug"
fi

# Execute the application
exec ./bin/octi-sync-server-kotlin $CMD_ARGS
