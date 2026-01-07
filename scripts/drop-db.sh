#!/bin/bash

# Drop the database for development purposes
DB_NAME=${POSTGRES_DB:-heartandfear}
DB_USER=${POSTGRES_USER:-postgres}

echo "WARNING: This will drop the entire database '$DB_NAME' and all its data!"
read -p "Are you sure you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Operation cancelled."
    exit 1
fi

echo "Dropping database '$DB_NAME'..."
docker-compose exec postgres psql -U "$DB_USER" -c "DROP DATABASE IF EXISTS $DB_NAME;"
docker-compose exec postgres psql -U "$DB_USER" -c "CREATE DATABASE $DB_NAME;"

echo "Database '$DB_NAME' has been dropped and recreated successfully!"
