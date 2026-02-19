#!/bin/bash

# Test client for Kubernetes tunnel server

SERVER_URL="http://localhost:9001"

echo "Testing Kubernetes tunnel server..."
echo "===================================="
echo ""

echo "1. Testing health endpoint (local mock response):"
curl -s "$SERVER_URL/health" | jq .
echo ""

echo "2. Testing relay endpoint (via kubectl tunnel):"
echo "   Note: This will forward to the Kubernetes pod via kubectl port-forward"
echo "   Expected: Response from the pod in namespace 'default' with prefix 'my-app-'"
echo ""
curl -v "$SERVER_URL/api/users" 2>&1 | head -30
echo ""

echo "===================================="
echo "Test complete."
echo ""
echo "To test with kubectl manually:"
echo "  kubectl get pods -n default | grep my-app-"
echo "  kubectl port-forward pod/<pod-name> 8080:8080 -n default"
