#!/bin/bash

# Connect to PostgreSQL database using docker exec
DB_NAME=${POSTGRES_DB:-heartandfear}
DB_USER=${POSTGRES_USER:-postgres}

echo "Connecting to database '$DB_NAME' as user '$DB_USER'..."
docker compose exec postgres psql -U "$DB_USER" -d "$DB_NAME"
