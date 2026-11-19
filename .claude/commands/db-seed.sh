#!/bin/bash
# Description: Seed database with sample shortened URLs for testing

echo "=== Seeding Database with Sample URLs ==="
BASE_URL="http://localhost:8080"
AUTH="admin:admin123"

# Sample URLs to shorten
declare -a URLS=(
    "https://www.google.com"
    "https://github.com"
    "https://stackoverflow.com"
    "https://www.youtube.com"
    "https://www.wikipedia.org"
    "https://www.reddit.com"
    "https://www.amazon.com"
    "https://www.twitter.com"
    "https://www.linkedin.com"
    "https://www.facebook.com"
)

echo "Creating shortened URLs..."
for url in "${URLS[@]}"; do
    echo -n "Shortening: $url ... "
    RESPONSE=$(curl -s -u "$AUTH" -X POST "$BASE_URL/api/shorten" \
        -H "Content-Type: application/json" \
        -d "{\"originalUrl\":\"$url\"}")
    SHORT_CODE=$(echo "$RESPONSE" | jq -r '.shortCode // empty')
    if [ -n "$SHORT_CODE" ]; then
        echo "✓ $SHORT_CODE"
    else
        echo "✗ Failed"
    fi
done

echo -e "\n=== Seeding Complete ==="
echo "View all URLs: curl -u $AUTH $BASE_URL/api/urls | jq"
