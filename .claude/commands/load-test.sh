#!/bin/bash
# Description: Run basic load test on URL shortener API

BASE_URL="http://localhost:8080"
AUTH="admin:admin123"
REQUESTS=100
CONCURRENT=10

echo "=== Load Testing URL Shortener ==="
echo "Total requests: $REQUESTS"
echo "Concurrent: $CONCURRENT"
echo ""

# Check if Apache Bench (ab) is available
if ! command -v ab &> /dev/null; then
    echo "Apache Bench (ab) not found. Install with: brew install apache2"
    echo "Falling back to simple curl loop..."

    START=$(date +%s)
    for i in $(seq 1 $REQUESTS); do
        curl -s -u "$AUTH" -X POST "$BASE_URL/api/urls" \
            -H "Content-Type: application/json" \
            -d "{\"originalUrl\":\"https://example.com/test$i\"}" > /dev/null
        if [ $((i % 10)) -eq 0 ]; then
            echo "Completed $i requests..."
        fi
    done
    END=$(date +%s)
    DURATION=$((END - START))
    RPS=$((REQUESTS / DURATION))
    echo "Duration: ${DURATION}s"
    echo "Requests/sec: $RPS"
else
    # Use Apache Bench for proper load testing
    echo "{\"originalUrl\":\"https://example.com/loadtest\"}" > /tmp/post_data.json
    ab -n $REQUESTS -c $CONCURRENT -p /tmp/post_data.json \
        -T "application/json" \
        -A "$AUTH" \
        "$BASE_URL/api/urls"
    rm /tmp/post_data.json
fi

echo -e "\n=== Load Test Complete ==="
