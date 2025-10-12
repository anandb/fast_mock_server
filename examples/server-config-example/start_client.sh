#!/bin/bash

# Test client script for server-config-example servers
# This script demonstrates calling both the api-server and secure-server endpoints

API_BASE_URL="http://localhost:8081"
SECURE_BASE_URL="https://localhost:8443"

echo "======================================"
echo "Testing API Server (port 8081)"
echo "======================================"
echo ""

# Test 1: GET /api/users
echo "Test 1: GET /api/users - List all users"
echo "Request: curl -X GET $API_BASE_URL/api/users"
curl -X GET "$API_BASE_URL/api/users" -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 2: GET /api/users/1
echo "Test 2: GET /api/users/1 - Get specific user"
echo "Request: curl -X GET $API_BASE_URL/api/users/1"
curl -X GET "$API_BASE_URL/api/users/1" -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 3: POST /api/users
echo "Test 3: POST /api/users - Create new user"
echo "Request: curl -X POST $API_BASE_URL/api/users -H 'Content-Type: application/json' -d '{\"name\":\"Test User\"}'"
curl -X POST "$API_BASE_URL/api/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User"}' \
  -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

echo "======================================"
echo "Testing Secure Server (port 8443)"
echo "======================================"
echo "Note: This server requires TLS/SSL certificates."
echo "The example config uses placeholder paths. Update the paths in the config file."
echo ""

# Test 4: GET /secure/data (will fail without proper certs)
echo "Test 4: GET /secure/data - Secure endpoint"
echo "Request: curl -X GET $SECURE_BASE_URL/secure/data -k"
echo "(Using -k to skip certificate verification for demo purposes)"
curl -X GET "$SECURE_BASE_URL/secure/data" -k -w "\nStatus Code: %{http_code}\n" -s 2>/dev/null || echo "Failed to connect. Server may not be running with valid certificates."
echo ""
echo "--------------------------------------"
echo ""
