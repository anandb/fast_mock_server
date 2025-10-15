#!/bin/bash

# Path Variables Example - Start Server Script
# This script starts the mock server with path variable support examples

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"

echo "Starting Mock Server with Path Variables example configuration..."
echo "Configuration: $SCRIPT_DIR/server-config-pathvars-example.jsonmc"
echo ""

cd "$PROJECT_ROOT"

# Build the project if needed
if [ ! -f "target/mock-server-1.0.0.jar" ]; then
    echo "Building project..."
    mvn clean package -DskipTests
fi

# Start the server with the configuration
java -Dmock.server.config.file="$SCRIPT_DIR/server-config-pathvars-example.jsonmc" -Djava.util.logging.config.class=org.slf4j.bridge.SLF4JBridgeHandler.class\
     -jar target/mock-server-1.0.0.jar
