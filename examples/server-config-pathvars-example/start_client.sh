#!/bin/bash

# Path Variables Example - Test Client Script
# This script sends test requests to demonstrate path variable functionality

BASE_URL="http://localhost:9090"

echo "=================================="
echo "Path Variables Example - Test Client"
echo "=================================="
echo ""
echo "Make sure the server is running with: ./start_server.sh"
echo ""

# Test 1: Simple path variable
echo "Test 1: GET /users/{id}"
echo "Request: GET /users/123"
curl -s -X GET "$BASE_URL/users/123" \
     -H "X-User-Name: JohnDoe" | jq '.'
echo ""
echo "---"
echo ""

# Test 2: Multiple path variables
echo "Test 2: GET /users/{userId}/posts/{postId}"
echo "Request: GET /users/456/posts/789"
curl -s -X GET "$BASE_URL/users/456/posts/789" \
     -H "X-Author-Name: Jane Smith" | jq '.'
echo ""
echo "---"
echo ""

# Test 3: Complex path with POST request
echo "Test 3: POST /api/v1/organizations/{orgId}/projects/{projectId}/tasks"
echo "Request: POST /api/v1/organizations/org123/projects/proj456/tasks"
curl -s -X POST "$BASE_URL/api/v1/organizations/org123/projects/proj456/tasks" \
     -H "Content-Type: application/json" \
     -H "X-User-Id: admin-001" \
     -d '{
       "title": "Implement new feature",
       "description": "Add path variable support",
       "assignee": "developer@example.com",
       "priority": "high"
     }' | jq '.'
echo ""
echo "---"
echo ""

# Test 4: PUT request with path variable
echo "Test 4: PUT /users/{id}/profile"
echo "Request: PUT /users/789/profile"
curl -s -X PUT "$BASE_URL/users/789/profile" \
     -H "Content-Type: application/json" \
     -H "X-Admin-Id: admin-002" \
     -d '{
       "name": "Alice Johnson",
       "email": "alice@example.com",
       "age": 28
     }' | jq '.'
echo ""
echo "---"
echo ""

# Test 5: DELETE request with multiple path variables
echo "Test 5: DELETE /users/{userId}/comments/{commentId}"
echo "Request: DELETE /users/101/comments/202"
curl -s -X DELETE "$BASE_URL/users/101/comments/202" \
     -H "X-User-Id: moderator-001" | jq '.'
echo ""
echo "---"
echo ""

# Test 6: Multiple path variables in different positions
echo "Test 6: GET /api/v2/products/{category}/{productId}/reviews"
echo "Request: GET /api/v2/products/electronics/laptop-x1/reviews"
curl -s -X GET "$BASE_URL/api/v2/products/electronics/laptop-x1/reviews" \
     -H "User-Agent: TestClient/1.0" | jq '.'
echo ""
echo "---"
echo ""

echo "=================================="
echo "All tests completed!"
echo "=================================="
