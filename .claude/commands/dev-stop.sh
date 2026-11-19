#!/bin/bash
# Description: Stop the application and Gradle daemon
echo "Stopping Gradle daemon..."
./gradlew --stop
echo "To stop the running application, press Ctrl+C or: pkill -f 'gradle.*bootRun'"
