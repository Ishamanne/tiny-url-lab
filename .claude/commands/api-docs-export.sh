#!/bin/bash
# Description: Export OpenAPI specification to file

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_DIR="api-docs"
OUTPUT_FILE="$OUTPUT_DIR/openapi_$TIMESTAMP.json"
LATEST_FILE="$OUTPUT_DIR/openapi_latest.json"

mkdir -p "$OUTPUT_DIR"

echo "=== Exporting OpenAPI Specification ==="

# Fetch OpenAPI spec
curl -s http://localhost:8080/v3/api-docs > "$OUTPUT_FILE"

if [ -s "$OUTPUT_FILE" ]; then
    echo "✓ API spec exported to: $OUTPUT_FILE"

    # Also save as latest
    cp "$OUTPUT_FILE" "$LATEST_FILE"

    # Print summary
    echo -e "\nAPI Summary:"
    echo "  Title: $(jq -r '.info.title' "$OUTPUT_FILE")"
    echo "  Version: $(jq -r '.info.version' "$OUTPUT_FILE")"
    echo "  Endpoints: $(jq -r '.paths | keys | length' "$OUTPUT_FILE")"

    # Pretty print to YAML format if yq is available
    if command -v yq &> /dev/null; then
        YAML_FILE="${OUTPUT_FILE%.json}.yaml"
        yq -P "$OUTPUT_FILE" > "$YAML_FILE"
        echo "✓ YAML version: $YAML_FILE"
    fi

    echo -e "\nView Swagger UI: http://localhost:8080/swagger-ui.html"
else
    echo "✗ Export failed - is the application running?"
    rm -f "$OUTPUT_FILE"
    exit 1
fi
