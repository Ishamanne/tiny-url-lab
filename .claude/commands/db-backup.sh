#!/bin/bash
# Description: Export H2 database data to SQL file

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="backups"
BACKUP_FILE="$BACKUP_DIR/db_backup_$TIMESTAMP.sql"

mkdir -p "$BACKUP_DIR"

echo "=== Backing Up H2 Database ==="
echo "Note: This requires the application to be running with H2 console enabled"

# Try to export via H2 console API
# Since H2 is in-memory, we'll export all data via the REST API instead
BASE_URL="http://localhost:8080"
AUTH="admin:admin123"

echo "Fetching all shortened URLs..."
curl -s -u "$AUTH" "$BASE_URL/api/urls" | jq > "$BACKUP_FILE"

if [ -s "$BACKUP_FILE" ]; then
    echo "✓ Backup saved to: $BACKUP_FILE"
    echo "Size: $(du -h "$BACKUP_FILE" | cut -f1)"
    echo "Records: $(jq 'length' "$BACKUP_FILE")"
else
    echo "✗ Backup failed - is the application running?"
    rm -f "$BACKUP_FILE"
    exit 1
fi

echo -e "\nTo restore, use the API to recreate URLs from this backup file"
