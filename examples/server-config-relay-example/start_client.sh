#!/bin/bash

# Test client script for server-config-relay-example
# This script demonstrates calling the relay server which forwards requests to a remote API

BASE_URL="http://localhost:8090"

echo "======================================"
echo "Testing Relay Server with OAuth2"
echo "======================================"
echo "Server: $BASE_URL"
echo ""
echo "NOTE: This relay server forwards requests to https://api.example.com"
echo "      Update the configuration with real credentials and remote URL to test."
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

echo "The relay server will:"
echo "1. Obtain an OAuth2 access token from the token endpoint"
echo "2. Add 'Authorization: Bearer <token>' to the request"
echo "3. Forward the request to the remote API"
echo "4. Return the response from the remote API"
echo ""
