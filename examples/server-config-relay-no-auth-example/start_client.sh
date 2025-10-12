#!/bin/bash

# Test client script for server-config-relay-no-auth-example
# This script demonstrates calling the relay server (without OAuth2)

BASE_URL="http://localhost:8090"

echo "======================================"
echo "Testing Relay Server (No Auth)"
echo "======================================"
echo "Server: $BASE_URL"
echo ""
echo "NOTE: This relay server forwards requests to https://api.example.com"
echo "      without adding any authentication. Update the configuration with"
echo "      the real remote URL to test."
echo ""

# Test 1: GET request through relay
echo "Test 1: GET /api/users - Relayed to remote server"
echo "Request: curl -X GET $BASE_URL/api/users"
curl -X GET "$BASE_URL/api/users" \
  -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 2: POST request through relay
echo "Test 2: POST /api/users - Create user via relay"
echo "Request: curl -X POST $BASE_URL/api/users -H 'Content-Type: application/json' -d '{...}'"
curl -X POST "$BASE_URL/api/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com"}' \
  -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 3: Custom endpoint through relay
echo "Test 3: GET /api/data - Custom endpoint via relay"
echo "Request: curl -X GET $BASE_URL/api/data"
curl -X GET "$BASE_URL/api/data" \
  -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 4: DELETE request through relay
echo "Test 4: DELETE /api/users/123 - Delete user via relay"
echo "Request: curl -X DELETE $BASE_URL/api/users/123"
curl -X DELETE "$BASE_URL/api/users/123" \
  -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

echo "The relay server will:"
echo "1. Forward the request to the remote API as-is"
echo "2. Add any custom headers configured in relayConfig.headers"
echo "3. Return the response from the remote API"
echo ""
echo "No authentication is added by the relay server."
echo ""
