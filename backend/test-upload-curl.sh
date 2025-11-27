#!/bin/bash

# Test file upload endpoint with curl
# This helps identify if the issue is with the server or the Android app

echo "🧪 Testing file upload endpoint..."
echo ""

# Replace with your actual auth token
AUTH_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiI2OTI3OTRjYzUyYjI1ZGY2ZGNjZGM5NDciLCJpYXQiOjE3NjQyMDE2NzYsImV4cCI6MTc2Njc5MzY3Nn0.R7aD05n877uBrDEcqf-7KNsjKr2Vfdxp27C69n3DFpk"

# Create a small test image (1x1 pixel PNG)
echo "📸 Creating test image..."
echo -n "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" | base64 -d > /tmp/test-image.png

echo "✅ Test image created: /tmp/test-image.png"
echo ""

# Test 1: Test endpoint
echo "🧪 Test 1: Testing test endpoint..."
curl -X POST http://localhost:9090/api/community/posts/test \
  -H "Content-Type: application/json" \
  -v \
  2>&1 | grep -E "(< HTTP|success|error)"

echo ""
echo ""

# Test 2: Post without image
echo "🧪 Test 2: Posting without image..."
curl -X POST http://localhost:9090/api/community/posts \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -F "content=Test post from curl" \
  -F "postType=GENERAL" \
  -v \
  2>&1 | tee /tmp/curl-test-no-image.log

echo ""
echo ""

# Test 3: Post with small image
echo "🧪 Test 3: Posting with small image..."
curl -X POST http://localhost:9090/api/community/posts \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -F "content=Test post with image from curl" \
  -F "postType=GENERAL" \
  -F "image=@/tmp/test-image.png" \
  -v \
  2>&1 | tee /tmp/curl-test-with-image.log

echo ""
echo ""

# Check results
echo "📊 Results:"
echo ""

if grep -q "HTTP.*200\|HTTP.*201" /tmp/curl-test-no-image.log; then
    echo "✅ Test 2 (no image): SUCCESS"
else
    echo "❌ Test 2 (no image): FAILED"
    echo "   Check logs in /tmp/curl-test-no-image.log"
fi

if grep -q "HTTP.*200\|HTTP.*201" /tmp/curl-test-with-image.log; then
    echo "✅ Test 3 (with image): SUCCESS"
else
    echo "❌ Test 3 (with image): FAILED"
    echo "   Check logs in /tmp/curl-test-with-image.log"
fi

echo ""
echo "📋 Full logs saved to:"
echo "   /tmp/curl-test-no-image.log"
echo "   /tmp/curl-test-with-image.log"

# Cleanup
rm -f /tmp/test-image.png

echo ""
echo "✅ Tests complete"

