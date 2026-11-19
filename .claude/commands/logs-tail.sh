#!/bin/bash
# Description: Tail application logs with optional filtering
LOG_FILE="logs/application.log"

if [ ! -f "$LOG_FILE" ]; then
    echo "No log file found. Showing Gradle bootRun output instead..."
    ./gradlew bootRun 2>&1 | grep --color=auto -E "ERROR|WARN|INFO|$"
else
    echo "=== Tailing application logs (Ctrl+C to stop) ==="
    tail -f "$LOG_FILE" | grep --color=auto -E "ERROR|WARN|INFO|$"
fi
