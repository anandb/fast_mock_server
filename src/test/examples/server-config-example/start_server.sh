#!/bin/bash

# Start the mock server with server-config-example.jsonmc configuration
# This example includes two servers:
# - api-server on port 9001
# - secure-server on port 8443 (HTTPS)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
CONFIG_FILE="$SCRIPT_DIR/server-config-example.jsonmc"

echo "Starting mock servers from server-config-example.jsonmc..."
echo "  - api-server on port 9001 (HTTP)"
echo "  - secure-server on port 8443 (HTTPS)"
echo "Configuration: $CONFIG_FILE"
echo ""

cd "$PROJECT_ROOT"

# Check if the JAR file exists
if [ ! -f target/*.jar ]; then
    echo "Error: JAR file not found. Please build the project first with: mvn clean package"
    exit 1
fi

# Start the server with the configuration file
java -Dmock.server.config.file="$CONFIG_FILE" -Djava.util.logging.config.class=org.slf4j.bridge.SLF4JBridgeHandler.class\
     -jar target/mock-server-1.0.0.jar
