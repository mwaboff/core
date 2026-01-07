#!/bin/bash

# Generate a blank Flyway migration file
# Usage: ./create-migration.sh migration_name

if [ $# -eq 0 ]; then
    echo "Usage: $0 <migration_name>"
    echo "Example: $0 create_users_table"
    exit 1
fi

MIGRATION_NAME=$1
MIGRATION_DIR="src/main/resources/db/migration"

# Create migration directory if it doesn't exist
mkdir -p "$MIGRATION_DIR"

# Generate timestamp in VyyyyMMddHHmmssSSS format
TIMESTAMP=$(date +"V%Y%m%d%H%M%S%3N")
FILENAME="${TIMESTAMP}__${MIGRATION_NAME}.sql"
FILEPATH="$MIGRATION_DIR/$FILENAME"

# Create the migration file with basic template
cat > "$FILEPATH" << EOF
-- Migration: $MIGRATION_NAME
-- Created: $(date)

-- Add your SQL statements here

EOF

echo "Created migration file: $FILEPATH"
echo "Migration name: $MIGRATION_NAME"
echo "Timestamp: $TIMESTAMP"
