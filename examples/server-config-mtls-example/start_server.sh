#!/bin/bash

# Start the mock server with mutual TLS (mTLS) configuration
# This example demonstrates HTTPS with client certificate verification

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_FILE="$SCRIPT_DIR/server-config-mtls-example.jsonmc"

echo "Starting mtls-secure-api server on port 8443..."
echo "This server requires mutual TLS authentication (client certificates)"
echo "Configuration: $CONFIG_FILE"
echo ""
echo "Note: The server uses embedded certificates from the config file."
echo "      Client certificates are also included in this directory."
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
