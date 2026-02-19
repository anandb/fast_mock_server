#!/bin/bash

# Start server with Kubernetes tunnel configuration

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
JAR_FILE="$PROJECT_ROOT/target/mock-server-1.0.0.jar"
CONFIG_FILE="$SCRIPT_DIR/server-config-tunnel-example.jsonmc"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found. Building project..."
    cd "$PROJECT_ROOT" && mvn clean package -DskipTests
fi

echo "Starting mock server with Kubernetes tunnel configuration..."
echo "Configuration: $CONFIG_FILE"
echo ""

java -Dmock.server.config.file="$CONFIG_FILE" -jar "$JAR_FILE"
