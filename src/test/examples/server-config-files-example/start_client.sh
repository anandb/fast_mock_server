#!/bin/bash

# Test client script for server-config-files-example server
# This script demonstrates calling the file server endpoints

set -eo pipefail
API_BASE_URL="http://localhost:8082"

echo "======================================"
echo "Testing File Server (port 8082)"
echo "======================================"
echo "NOTE: The server uses placeholder file paths."
echo "Update the paths in server-config-files-example.jsonmc to test actual file downloads."
echo ""
echo "--------------------------------------"
echo ""

# Test 1: GET /api/download/documents
echo "Test 1: GET /api/download/documents - Multi-part document download (2 PDFs)"
echo "Request: curl -X GET $API_BASE_URL/api/download/documents/{id}"
curl -f -X GET "$API_BASE_URL/api/download/documents/Fibre" -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 2: GET /api/download/images
echo "Test 2: GET /api/download/images - Multi-part image download (3 images)"
echo "Request: curl -X GET $API_BASE_URL/api/download/images"
curl -f -X GET "$API_BASE_URL/api/download/images" -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 3: POST /api/export/reports
echo "Test 3: POST /api/export/reports - Multi-part report export (CSV, XLSX, JSON)"
echo "Request: curl -X POST $API_BASE_URL/api/export/reports"
curl -f -X POST "$API_BASE_URL/api/export/reports" -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 4: GET /api/download/archive
echo "Test 4: GET /api/download/archive - Single file download (ZIP)"
echo "Request: curl -X GET $API_BASE_URL/api/download/archive"
curl -f -X GET "$API_BASE_URL/api/download/archive" -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

# Test 5: GET /api/regular/data
echo "Test 5: GET /api/regular/data - Regular JSON response (no file download)"
echo "Request: curl -X GET $API_BASE_URL/api/regular/data"
curl -f -X GET "$API_BASE_URL/api/regular/data" -w "\nStatus Code: %{http_code}\n" -s
echo ""
echo "--------------------------------------"
echo ""

echo "======================================"
echo "Testing Complete"
echo "======================================"
echo ""
echo "To save downloaded files, use the -o option with curl:"
echo "  curl -X GET $API_BASE_URL/api/download/archive -o archive.zip"
echo ""
