#!/bin/bash

# Start the mock server with relay configuration (no authentication)
# This example demonstrates a server that relays requests to a remote API without OAuth2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_FILE="$SCRIPT_DIR/server-config-relay-no-auth-example.jsonmc"

echo "Starting relay-server-no-auth on port 8090..."
echo "This server relays all requests to https://api.example.com"
echo "Authentication: None (simple proxy)"
echo "Configuration: $CONFIG_FILE"
echo ""
echo "NOTE: Update the configuration file with the real remote URL before using."
echo ""

cd "$PROJECT_ROOT"

# Check if the JAR file exists
if [ ! -f target/*.jar ]; then
    echo "Error: JAR file not found. Please build the project first with: mvn clean package"
    exit 1
fi

# Start the server with the configuration file
java -jar target/*.jar -Dmock.server.config.file="$CONFIG_FILE"
