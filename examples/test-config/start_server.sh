#!/bin/bash

# Start the mock server with test-config.jsonmc configuration
# This script assumes the mock server JAR is built and available in the target directory

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CONFIG_FILE="$SCRIPT_DIR/test-config.jsonmc"

echo "Starting test-api server on port 9001..."
echo "Configuration: $CONFIG_FILE"
echo ""

cd "$PROJECT_ROOT"

# Check if the JAR file exists
if [ ! -f target/*.jar ]; then
    echo "Error: JAR file not found. Please build the project first with: mvn clean package"
    exit 1
fi

# Start the server with the configuration file
java -jar target/*.jar -Dmock.server.config.file="$CONFIG_FILE"
