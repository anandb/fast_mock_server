#!/bin/bash

# Start the mock server with server-config-files-example.jsonmc configuration
# This example demonstrates multi-part file downloads
# - file-server on port 8082

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_FILE="$SCRIPT_DIR/server-config-files-example.jsonmc"

echo "Starting mock server from server-config-files-example.jsonmc..."
echo "  - file-server on port 8082 (HTTP)"
echo "Configuration: $CONFIG_FILE"
echo ""
echo "NOTE: This example uses placeholder file paths."
echo "Update the file paths in the configuration to point to actual files on your system."
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
