#!/bin/bash

# Start the mock server with server-config-sse-example.jsonmc configuration
# This example demonstrates Server-Sent Events (SSE) support
# - sse-server on port 9002

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
CONFIG_FILE="$SCRIPT_DIR/server-config-sse-example.jsonmc"

echo "Starting SSE mock server from server-config-sse-example.jsonmc..."
echo "  - sse-server on port 9002 (HTTP)"
echo "Configuration: $CONFIG_FILE"
echo ""
echo "Available SSE endpoints:"
echo "  GET http://localhost:9002/api/stream - Simple text messages"
echo "  GET http://localhost:9002/api/notifications - JSON notification events"
echo "  GET http://localhost:9002/api/progress - Progress tracking events"
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
