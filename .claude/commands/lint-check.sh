#!/bin/bash
# Description: Run code quality and style checks

echo "=== Running Code Quality Checks ==="

# Check if Checkstyle plugin is available
echo -e "\n1. Checkstyle Analysis..."
./gradlew checkstyleMain checkstyleTest 2>/dev/null || echo "Checkstyle not configured"

# SpotBugs (formerly FindBugs)
echo -e "\n2. SpotBugs Analysis..."
./gradlew spotbugsMain 2>/dev/null || echo "SpotBugs not configured"

# PMD
echo -e "\n3. PMD Analysis..."
./gradlew pmdMain 2>/dev/null || echo "PMD not configured"

# Compile check
echo -e "\n4. Compilation Check..."
./gradlew compileJava compileTestJava

echo -e "\n=== Quality Check Complete ==="
echo "To add quality plugins, add to build.gradle:"
echo "  - Checkstyle: id 'checkstyle'"
echo "  - SpotBugs: id 'com.github.spotbugs' version '5.0.14'"
echo "  - PMD: id 'pmd'"
