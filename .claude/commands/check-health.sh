#!/bin/bash
# Description: Check if app and dependencies are healthy
echo "=== Application Health ==="
curl -s http://localhost:8080/actuator/health | jq
echo -e "\n=== Available Metrics ==="
curl -s http://localhost:8080/actuator/metrics | jq '.names[]' | head -10
