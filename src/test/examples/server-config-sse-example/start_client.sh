#!/bin/bash

# Test client script for server-config-sse-example server
# This script demonstrates calling the SSE endpoints

set -eo pipefail

API_BASE_URL="http://localhost:9002"

echo "======================================"
echo "Testing SSE Server (port 9002)"
echo "======================================"
echo ""

# Test 1: GET /api/stream
echo "Test 1: GET /api/stream - Simple text SSE messages"
echo "Request: curl -X GET $API_BASE_URL/api/stream"
echo "Expected: 3 text messages sent as SSE events"
echo ""
curl -N -X GET "$API_BASE_URL/api/stream" -H "Accept: text/event-stream" 2>/dev/null
echo ""
echo "--------------------------------------"
echo ""

# Test 2: GET /api/notifications
echo "Test 2: GET /api/notifications - JSON notification events"
echo "Request: curl -X GET $API_BASE_URL/api/notifications"
echo "Expected: 4 JSON notification events sent as SSE"
echo ""
curl -N -X GET "$API_BASE_URL/api/notifications" -H "Accept: text/event-stream" 2>/dev/null
echo ""
echo "--------------------------------------"
echo ""

# Test 3: GET /api/progress
echo "Test 3: GET /api/progress - Progress tracking events"
echo "Request: curl -X GET $API_BASE_URL/api/progress"
echo "Expected: 5 progress update events sent as SSE"
echo ""
curl -N -X GET "$API_BASE_URL/api/progress" -H "Accept: text/event-stream" 2>/dev/null
echo ""
echo "--------------------------------------"
echo ""

echo "======================================"
echo "All SSE tests completed!"
echo "======================================"
echo ""
echo "Note: SSE events are sent in text/event-stream format."
echo "Each message is prefixed with 'data:' and followed by two newlines."
echo "The mock server sends all messages immediately (no actual delay)."
echo "The 'interval' field is informational and indicates the intended delay."
echo ""
