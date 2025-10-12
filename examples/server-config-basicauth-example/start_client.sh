#!/bin/bash

# Test client script for server-config-basicauth-example
# This script demonstrates calling endpoints with basic authentication

BASE_URL="http://localhost:9090"
USERNAME="admin"
PASSWORD="secret123"

echo "======================================"
echo "Testing API Server with Basic Auth"
echo "======================================"
echo "Credentials: username=$USERNAME, password=$PASSWORD"
echo ""

# Test 1: GET /api/users with authentication
echo "Test 1: GET /api/users - List all users (with auth)"
echo "Request: curl -X GET $BASE_URL/api/users -u $USERNAME:$PASSWORD"
curl -X GET "$BASE_URL/api/users" \
  -u "$USERNAME:$PASSWORD" \
  -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 2: GET /api/users without authentication (should fail)
echo "Test 2: GET /api/users - Without authentication (should return 401)"
echo "Request: curl -X GET $BASE_URL/api/users"
curl -X GET "$BASE_URL/api/users" \
  -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 3: POST /api/users with authentication
echo "Test 3: POST /api/users - Create new user (with auth)"
echo "Request: curl -X POST $BASE_URL/api/users -u $USERNAME:$PASSWORD -H 'Content-Type: application/json' -d '{\"name\":\"Test User\"}'"
curl -X POST "$BASE_URL/api/users" \
  -u "$USERNAME:$PASSWORD" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com"}' \
  -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 4: Using Authorization header directly
echo "Test 4: GET /api/users - Using Authorization header directly"
AUTH_HEADER=$(echo -n "$USERNAME:$PASSWORD" | base64)
echo "Request: curl -X GET $BASE_URL/api/users -H 'Authorization: Basic $AUTH_HEADER'"
curl -X GET "$BASE_URL/api/users" \
  -H "Authorization: Basic $AUTH_HEADER" \
  -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""
