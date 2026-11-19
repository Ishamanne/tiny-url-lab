#!/bin/bash
# Description: Run security vulnerability scan on dependencies

echo "=== Running Dependency Security Check ==="
echo "This may take a few minutes on first run..."
./gradlew dependencyCheckAnalyze

echo -e "\n=== Scan Complete ==="
REPORT_PATH="build/reports/dependency-check-report.html"
if [ -f "$REPORT_PATH" ]; then
    echo "Report generated: $REPORT_PATH"
    echo "Open report: open $REPORT_PATH"
else
    echo "No report generated. Check if OWASP Dependency-Check plugin is configured in build.gradle"
fi
