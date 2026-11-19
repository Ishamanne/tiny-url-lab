#!/bin/bash
# Description: Build production-ready JAR
./gradlew clean build -x test
echo "Build complete! JAR location:"
ls -lh build/libs/*.jar
