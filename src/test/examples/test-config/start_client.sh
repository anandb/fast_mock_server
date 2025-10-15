#!/bin/bash

# Test client script for test-config server
# This script demonstrates calling the test-api endpoints

set -eo pipefail

BASE_URL="http://localhost:9001"

echo "======================================"
echo "Testing test-api Server"
echo "======================================"
echo ""

# Test 1: GET /test
echo "Test 1: GET /test"
echo "Request: curl -X GET $BASE_URL/test"
curl -f -X GET "$BASE_URL/test" -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""
