#!/bin/bash
# Description: Export current metrics to timestamped file

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_DIR="metrics-snapshots"
OUTPUT_FILE="$OUTPUT_DIR/metrics_$TIMESTAMP.json"

mkdir -p "$OUTPUT_DIR"

echo "=== Capturing Metrics Snapshot ==="
echo "Timestamp: $(date)"

# Capture all metrics
curl -s http://localhost:8080/actuator/metrics | jq > "$OUTPUT_FILE"

# Capture specific important metrics
echo "{" > "$OUTPUT_DIR/detailed_$TIMESTAMP.json"
echo "  \"timestamp\": \"$(date -u +"%Y-%m-%dT%H:%M:%SZ")\"," >> "$OUTPUT_DIR/detailed_$TIMESTAMP.json"
echo "  \"health\": $(curl -s http://localhost:8080/actuator/health)," >> "$OUTPUT_DIR/detailed_$TIMESTAMP.json"
echo "  \"jvm_memory\": $(curl -s http://localhost:8080/actuator/metrics/jvm.memory.used)," >> "$OUTPUT_DIR/detailed_$TIMESTAMP.json"
echo "  \"http_requests\": $(curl -s http://localhost:8080/actuator/metrics/http.server.requests)" >> "$OUTPUT_DIR/detailed_$TIMESTAMP.json"
echo "}" >> "$OUTPUT_DIR/detailed_$TIMESTAMP.json"

echo -e "\n=== Snapshot Saved ==="
echo "Metrics list: $OUTPUT_FILE"
echo "Detailed metrics: $OUTPUT_DIR/detailed_$TIMESTAMP.json"
ls -lh "$OUTPUT_DIR"/*.json | tail -5
