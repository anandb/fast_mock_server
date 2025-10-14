#!/bin/bash

# Start the mock server with basic authentication
# This example demonstrates a server protected by basic auth

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_FILE="$SCRIPT_DIR/server-config-basicauth-example.jsonmc"

echo "Starting api-server-with-auth on port 9001..."
echo "Basic Authentication: username=admin, password=secret123"
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
