#!/bin/bash

# Role modification script for Daggerheart TTRPG Application
# Usage: ./change-role.sh <user-id> <role>
# Example: ./change-role.sh 550e8400-e29b-41d4-a716-446655440000 ADMIN

set -e

# Check if required arguments are provided
if [ $# -ne 2 ]; then
    echo "Usage: $0 <user-id> <role>"
    echo "Valid roles: USER, MODERATOR, ADMIN, OWNER"
    echo "Example: $0 550e8400-e29b-41d4-a716-446655440000 ADMIN"
    exit 1
fi

USER_ID="$1"
ROLE="$2"

# Validate role
VALID_ROLES=("USER" "MODERATOR" "ADMIN" "OWNER")
if [[ ! " ${VALID_ROLES[@]} " =~ " ${ROLE} " ]]; then
    echo "Error: Invalid role '$ROLE'"
    echo "Valid roles: ${VALID_ROLES[*]}"
    exit 1
fi

# Validate UUID format (basic check)
if [[ ! $USER_ID =~ ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$ ]]; then
    echo "Error: Invalid UUID format: $USER_ID"
    exit 1
fi

# Database connection parameters
DB_NAME="${POSTGRES_DB:-heartandfear}"
DB_USER="${POSTGRES_USER:-postgres}"
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"

echo "Changing role for user $USER_ID to $ROLE..."

# Connect to database and update role
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << EOF
BEGIN;

-- Check if user exists
SELECT id, username, current_role FROM users WHERE id = '$USER_ID';

-- Update user role
UPDATE users 
SET role = '$ROLE', last_updated_date = CURRENT_TIMESTAMP 
WHERE id = '$USER_ID';

-- Create admin log entry
INSERT INTO admin_log (id, admin_user_id, target_user_id, action, details, ip_address, performed_at, created_date, last_updated_date)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM users WHERE username = 'system'),
    '$USER_ID',
    'CHANGE_ROLE',
    'Role changed via script to $ROLE',
    '127.0.0.1',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

COMMIT;

-- Verify the change
SELECT id, username, role, active FROM users WHERE id = '$USER_ID';
EOF

if [ $? -eq 0 ]; then
    echo "✅ Successfully changed role for user $USER_ID to $ROLE"
else
    echo "❌ Failed to change role for user $USER_ID"
    exit 1
fi
