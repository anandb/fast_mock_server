#!/bin/bash

# Test client script for server-config-mtls-example
# This script demonstrates calling mTLS endpoints with client certificates
set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_URL="https://localhost:8443"
CLIENT_CERT="$SCRIPT_DIR/client-cert.pem"
CLIENT_KEY="$SCRIPT_DIR/client-key.pem"
CA_CERT="$SCRIPT_DIR/ca-cert.pem"

echo "======================================"
echo "Testing mTLS Secure API Server"
echo "======================================"
echo "Server: $BASE_URL"
echo ""

# Check if client certificates exist
if [ ! -f "$CLIENT_CERT" ] || [ ! -f "$CLIENT_KEY" ]; then
    echo "Warning: Client certificates not found. Generating sample certificates..."
    echo "You may need to regenerate these with proper CA signing for production use."
    echo ""
fi

# Test 1: GET /secure/api/account with mTLS
echo "Test 1: GET /secure/api/account - Get account details (with client cert)"
echo "Request: curl -X GET $BASE_URL/secure/api/account --cert $CLIENT_CERT --key $CLIENT_KEY --cacert $CA_CERT"
curl -f -X GET "$BASE_URL/secure/api/account" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  --cacert "$CA_CERT" \
  -w "\nStatus Code: %{http_code}\n" -s 2>/dev/null || echo "Connection failed. Ensure server is running with valid certificates."
echo ""
echo "--------------------------------------"
echo ""

# Test 2: POST /secure/api/transaction with mTLS
echo "Test 2: POST /secure/api/transaction - Create transaction (with client cert)"
echo "Request: curl -X POST $BASE_URL/secure/api/transaction --cert $CLIENT_CERT --key $CLIENT_KEY --cacert $CA_CERT -H 'Content-Type: application/json' -d '{...}'"
curl -f -X POST "$BASE_URL/secure/api/transaction" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  --cacert "$CA_CERT" \
  -H "Content-Type: application/json" \
  -d '{"amount":500.00,"type":"transfer"}' \
  -w "\nStatus Code: %{http_code}\n" -s 2>/dev/null || echo "Connection failed."
echo ""
echo "--------------------------------------"
echo ""

# Test 3: GET /secure/api/health with mTLS
echo "Test 3: GET /secure/api/health - Health check (with client cert)"
echo "Request: curl -X GET $BASE_URL/secure/api/health --cert $CLIENT_CERT --key $CLIENT_KEY --cacert $CA_CERT"
curl -f -X GET "$BASE_URL/secure/api/health" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  --cacert "$CA_CERT" \
  -w "\nStatus Code: %{http_code}\n" -s 2>/dev/null || echo "Connection failed."
echo ""
echo "--------------------------------------"
echo ""

# Test 4: Attempt connection without client certificate (should fail)
echo "Test 4: GET /secure/api/account - Without client certificate (should fail)"
echo "Request: curl -X GET $BASE_URL/secure/api/account -k"
curl -f -X GET "$BASE_URL/secure/api/account" -k \
  -w "\nStatus Code: %{http_code}\n" -s 2>/dev/null || echo "Connection failed as expected (no client certificate)."
echo ""
echo "--------------------------------------"
echo ""
