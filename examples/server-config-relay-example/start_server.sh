#!/bin/bash

# Start the mock server with relay configuration (OAuth2)
# This example demonstrates a server that relays requests to a remote API with OAuth2 authentication

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_FILE="$SCRIPT_DIR/server-config-relay-example.jsonmc"

echo "Starting relay-server on port 8090..."
echo "This server relays all requests to https://api.example.com"
echo "Authentication: OAuth2 client credentials"
echo "Configuration: $CONFIG_FILE"
echo ""
echo "NOTE: Update the configuration file with real OAuth2 credentials and remote URL"
echo "      before using in production."
echo ""

cd "$PROJECT_ROOT"

# Check if the JAR file exists
if [ ! -f target/*.jar ]; then
    echo "Error: JAR file not found. Please build the project first with: mvn clean package"
    exit 1
fi

# Start the server with the configuration file
java -jar target/*.jar -Dmock.server.config.file="$CONFIG_FILE"
